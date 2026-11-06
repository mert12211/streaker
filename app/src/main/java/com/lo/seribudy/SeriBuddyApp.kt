package com.lo.seribudy

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.room.Room
import com.lo.seribudy.data.AppDatabase

class SeriBuddyApp : Application() {

    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "seri_buddy_db"
        ).fallbackToDestructiveMigration().build()

        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_SERVICE,
                "Seri Buddy Servisi",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Arka plan otomasyon servisi"
            }

            val alertChannel = NotificationChannel(
                CHANNEL_ALERTS,
                "Mesaj Bildirimleri",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Gönderilen ve başarısız mesaj bildirimleri"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(serviceChannel)
            notificationManager.createNotificationChannel(alertChannel)
        }
    }

    companion object {
        const val CHANNEL_SERVICE = "seri_buddy_service"
        const val CHANNEL_ALERTS = "seri_buddy_alerts"

        lateinit var instance: SeriBuddyApp
            private set
    }
}
