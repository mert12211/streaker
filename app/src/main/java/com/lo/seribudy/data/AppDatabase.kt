package com.lo.seribudy.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ScheduledMessage::class, StreakContact::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun contactDao(): ContactDao
}
