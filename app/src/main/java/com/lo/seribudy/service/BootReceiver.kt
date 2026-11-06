package com.lo.seribudy.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.lo.seribudy.SeriBuddyApp
import kotlinx.coroutines.*

/**
 * Telefon yeniden başlatıldığında sessiz zamanlayıcıları geri yükler.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("SeriBuddy_Boot", "Telefon açıldı, sessiz zamanlayıcılar geri yükleniyor...")

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val dao = SeriBuddyApp.instance.database.messageDao()
                    val activeMessages = dao.getActiveMessagesList()

                    for (message in activeMessages) {
                        AlarmReceiver.scheduleMessage(
                            context = context,
                            messageId = message.id,
                            recipientUser = message.recipientUsername,
                            recipientName = message.recipientDisplayName,
                            messageText = message.messageText,
                            targetDateMillis = message.targetDateMillis,
                            hour = message.hour,
                            minute = message.minute
                        )
                    }

                    Log.d("SeriBuddy_Boot", "${activeMessages.size} zamanlayıcı geri yüklendi")
                } catch (e: Exception) {
                    Log.e("SeriBuddy_Boot", "Zamanlayıcı yükleme hatası: ${e.message}")
                }
            }
        }
    }
}
