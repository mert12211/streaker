package com.lo.seribudy.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Zamanlanmış mesaj varlığı.
 * Kime, hangi tarihte, saat kaçta, ne gönderileceğini ve tekrar durumunu tutar.
 */
@Entity(tableName = "scheduled_messages")
data class ScheduledMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Seçilen seri dostunun kullanıcı adı / başlığı */
    val recipientUsername: String,

    /** Görünen ad */
    val recipientDisplayName: String,

    /** Gönderilecek mesaj metni (örn: "seri", "naber") */
    val messageText: String,

    /** Hedef tarih (Yıl-Ay-Gün epoch ms olarak) */
    val targetDateMillis: Long,

    /** Gönderilecek saat (0-23) */
    val hour: Int,

    /** Gönderilecek dakika (0-59) */
    val minute: Int,

    /** Tekrar modu: ONCE (tek seferlik), DAILY (her gün bu saatte), WEEKLY (her hafta) */
    val repeatType: RepeatType = RepeatType.ONCE,

    /** Görev aktif mi? */
    val isActive: Boolean = true,

    /** Son gönderim zamanı */
    val lastSentAt: Long? = null,

    /** Oluşturulma tarihi */
    val createdAt: Long = System.currentTimeMillis(),

    /** Durum */
    val status: MessageStatus = MessageStatus.PENDING
)

enum class RepeatType {
    ONCE,       // Belirtilen gün ve saatte tek sefer
    DAILY,      // Her gün bu saatte
    WEEKLY      // Her hafta bu gün ve saatte
}

enum class MessageStatus {
    PENDING,    // Zamanı bekliyor
    SENT,       // Arka planda başarıyla gönderildi
    FAILED,     // Başarısız
    CANCELLED   // İptal edildi
}
