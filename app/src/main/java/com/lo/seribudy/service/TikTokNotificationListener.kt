package com.lo.seribudy.service

import android.app.Notification
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.lo.seribudy.SeriBuddyApp
import com.lo.seribudy.data.StreakContact
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * TikTok ve Whee bildirimlerini dinleyen servis.
 * 1. Seri dostlarını ve gelen DM'leri tespit edip otomatik olarak rehbere ekler.
 * 2. Mümkün olduğunda Direct Reply (RemoteInput) üzerinden ekranı HİÇ açmadan
 *    tamamen arka planda mesaj gönderilmesini sağlar.
 */
class TikTokNotificationListener : NotificationListenerService() {

    companion object {
        private const val TAG = "SeriBuddy_Notif"
        var instance: TikTokNotificationListener? = null
            private set

        /**
         * Arka planda ekran açmadan doğrudan bildirim eylemi ile mesaj yanıtla
         */
        fun replyDirectly(contactName: String, replyText: String): Boolean {
            val service = instance ?: return false
            val activeNotifications = service.activeNotifications ?: return false

            for (sbn in activeNotifications) {
                val pkg = sbn.packageName
                if (pkg.contains("musically") || pkg.contains("trill") || pkg.contains("whee")) {
                    val extras = sbn.notification.extras
                    val title = extras.getString(Notification.EXTRA_TITLE) ?: ""

                    if (title.contains(contactName, ignoreCase = true)) {
                        // RemoteInput içeren Action'ı bul
                        val actions = sbn.notification.actions ?: continue
                        for (action in actions) {
                            val remoteInputs = action.remoteInputs ?: continue
                            for (remoteInput in remoteInputs) {
                                try {
                                    val intent = Intent()
                                    val bundle = Bundle()
                                    bundle.putCharSequence(remoteInput.resultKey, replyText)
                                    RemoteInput.addResultsToIntent(
                                        arrayOf(remoteInput),
                                        intent,
                                        bundle
                                    )
                                    action.actionIntent.send(service, 0, intent)
                                    Log.d(TAG, "Sessiz arka plan yanıtı gönderildi: $contactName -> $replyText")
                                    return true
                                } catch (e: Exception) {
                                    Log.e(TAG, "Doğrudan yanıt hatası: ${e.message}")
                                }
                            }
                        }
                    }
                }
            }
            return false
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        Log.d(TAG, "Bildirim dinleyici servisi bağlandı")
        scanExistingNotifications()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        instance = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val pkg = sbn.packageName
        if (pkg.contains("musically") || pkg.contains("trill") || pkg.contains("whee")) {
            processNotification(sbn)
        }
    }

    private fun scanExistingNotifications() {
        val notifications = activeNotifications ?: return
        for (sbn in notifications) {
            val pkg = sbn.packageName
            if (pkg.contains("musically") || pkg.contains("trill") || pkg.contains("whee")) {
                processNotification(sbn)
            }
        }
    }

    private fun processNotification(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

        if (title.isNotBlank()) {
            val isStreak = title.contains("🔥") || text.contains("🔥") ||
                    text.contains("seri", ignoreCase = true) ||
                    text.contains("streak", ignoreCase = true)

            val contact = StreakContact(
                username = title.trim(),
                displayName = title.trim(),
                hasStreak = isStreak,
                streakDays = if (isStreak) 5 else 1,
                lastMessage = text,
                updatedAt = System.currentTimeMillis()
            )

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    SeriBuddyApp.instance.database.contactDao().insertContact(contact)
                } catch (e: Exception) {
                    Log.e(TAG, "Kişi kaydedilirken hata: ${e.message}")
                }
            }
        }
    }
}
