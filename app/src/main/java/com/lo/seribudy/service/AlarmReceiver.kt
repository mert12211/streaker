package com.lo.seribudy.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.lo.seribudy.SeriBuddyApp
import com.lo.seribudy.data.MessageStatus
import com.lo.seribudy.data.RepeatType
import kotlinx.coroutines.*
import java.util.Calendar

/**
 * Sessiz Arka Plan Zamanlayıcı (Alarm sesi veya zil ÇALMAZ).
 * Belirlenen tarih ve saatte sistemi sessizce uyandırıp mesajı arka planda gönderir.
 */
class AlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SeriBuddy_SilentScheduler"
        const val EXTRA_MESSAGE_ID = "message_id"
        const val EXTRA_RECIPIENT_USER = "recipient_user"
        const val EXTRA_RECIPIENT_NAME = "recipient_name"
        const val EXTRA_MESSAGE_TEXT = "message_text"

        /**
         * Belirli bir tarih ve saat için sessiz zamanlayıcı kur
         */
        fun scheduleMessage(
            context: Context,
            messageId: Long,
            recipientUser: String,
            recipientName: String,
            messageText: String,
            targetDateMillis: Long,
            hour: Int,
            minute: Int
        ) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra(EXTRA_MESSAGE_ID, messageId)
                putExtra(EXTRA_RECIPIENT_USER, recipientUser)
                putExtra(EXTRA_RECIPIENT_NAME, recipientName)
                putExtra(EXTRA_MESSAGE_TEXT, messageText)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                messageId.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Hedef zamanı hesapla (Tarih + Saat)
            val calendar = Calendar.getInstance().apply {
                timeInMillis = targetDateMillis
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)

                // Eğer zaman geçmişse
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.timeInMillis,
                            pendingIntent
                        )
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.timeInMillis,
                            pendingIntent
                        )
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }

                Log.d(TAG, "Sessiz görev zamanlandı: $recipientName - Saat $hour:${minute.toString().padStart(2, '0')}")
            } catch (e: SecurityException) {
                Log.e(TAG, "Zamanlama yetki hatası: ${e.message}")
            }
        }

        /**
         * Zamanlamayı iptal et
         */
        fun cancelSchedule(context: Context, messageId: Long) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                messageId.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
            Log.d(TAG, "Zamanlama iptal edildi: $messageId")
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val messageId = intent.getLongExtra(EXTRA_MESSAGE_ID, -1)
        val recipientUser = intent.getStringExtra(EXTRA_RECIPIENT_USER) ?: return
        val recipientName = intent.getStringExtra(EXTRA_RECIPIENT_NAME) ?: recipientUser
        val messageText = intent.getStringExtra(EXTRA_MESSAGE_TEXT) ?: return

        Log.d(TAG, "Zamanı geldi! Arka planda sessizce gönderiliyor: $recipientName -> $messageText")

        // 1. Önce tamamen sessiz Direct Reply (bildirim üzerinden doğrudan yanıtlama) dene
        val repliedDirectly = TikTokNotificationListener.replyDirectly(recipientUser, messageText)
        if (repliedDirectly) {
            Log.d(TAG, "Ekran açılmadan bildirim üzerinden başarıyla yanıtlandı!")
            updateSuccess(messageId, context, recipientUser, recipientName, messageText)
            return
        }

        // 2. Direct Reply mevcut değilse Accessibility Servisi ile sessiz arka plan akışı başlat
        if (TikTokAccessibilityService.isServiceRunning) {
            TikTokAccessibilityService.sendMessageSilently(recipientUser, recipientName, messageText, messageId)
        } else {
            Log.w(TAG, "Erişilebilirlik servisi çalışmıyor.")
        }
    }

    private fun updateSuccess(
        messageId: Long,
        context: Context,
        recipientUser: String,
        recipientName: String,
        messageText: String
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val dao = SeriBuddyApp.instance.database.messageDao()
            dao.updateStatus(messageId, MessageStatus.SENT)

            val msg = dao.getMessageById(messageId)
            if (msg != null && msg.repeatType != RepeatType.ONCE) {
                val nextCalendar = Calendar.getInstance().apply {
                    timeInMillis = msg.targetDateMillis
                    if (msg.repeatType == RepeatType.DAILY) add(Calendar.DAY_OF_YEAR, 1)
                    if (msg.repeatType == RepeatType.WEEKLY) add(Calendar.WEEK_OF_YEAR, 1)
                }
                scheduleMessage(
                    context, messageId, recipientUser, recipientName, messageText,
                    nextCalendar.timeInMillis, msg.hour, msg.minute
                )
            }
        }
    }
}
