package com.lo.seribudy.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * TikTok / Whee Seri Sohbeti (DM Kişisi)
 * Kullanıcının manuel isim yazmasına gerek kalmadan ekranda listelenir.
 */
@Entity(tableName = "streak_contacts")
data class StreakContact(
    @PrimaryKey
    val username: String,

    /** Görünen ad */
    val displayName: String,

    /** Seri durumu (🔥 aktif mi?) */
    val hasStreak: Boolean = true,

    /** Kaç gündür seri yapılıyor (ör. 🔥 42) */
    val streakDays: Int = 1,

    /** Son mesaj özeti */
    val lastMessage: String = "",

    /** Son görülme veya güncelleme */
    val updatedAt: Long = System.currentTimeMillis()
)
