package com.lo.seribudy.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lo.seribudy.SeriBuddyApp
import com.lo.seribudy.data.*
import com.lo.seribudy.service.AlarmReceiver
import com.lo.seribudy.service.TikTokAccessibilityService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

data class MainUiState(
    val contacts: List<StreakContact> = emptyList(),
    val messages: List<ScheduledMessage> = emptyList(),
    val isWheeInstalled: Boolean = false,
    val isTikTokInstalled: Boolean = false,
    val isAccessibilityEnabled: Boolean = false,

    // Mesaj Zamanlama Alt Sayfası / Dialogu
    val selectedContact: StreakContact? = null,
    val showScheduleDialog: Boolean = false,

    // Form değerleri
    val messageText: String = "seri 🔥",
    val targetDateMillis: Long = System.currentTimeMillis(),
    val selectedHour: Int = 1,     // Varsayılan gece 01:00
    val selectedMinute: Int = 0,
    val selectedRepeat: RepeatType = RepeatType.DAILY
)

class MainViewModel : ViewModel() {

    private val db = SeriBuddyApp.instance.database
    private val contactDao = db.contactDao()
    private val messageDao = db.messageDao()

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        // Seri kişilerini dinle
        viewModelScope.launch {
            contactDao.getAllContacts().collect { list ->
                if (list.isEmpty()) {
                    // İlk açılışta kullanıcının hemen görebileceği başlangıç seri sohbetleri ekle
                    val defaults = listOf(
                        StreakContact("ceren_dm", "Ceren ✨", hasStreak = true, streakDays = 28, lastMessage = "seri"),
                        StreakContact("kaan_official", "Kaan 🔥", hasStreak = true, streakDays = 15, lastMessage = "alev"),
                        StreakContact("elif_yildiz", "Elif Yıldız", hasStreak = true, streakDays = 42, lastMessage = "seri dostum"),
                        StreakContact("emre_k", "Emre K.", hasStreak = true, streakDays = 7, lastMessage = "naber"),
                        StreakContact("selin_ak", "Selin", hasStreak = true, streakDays = 3, lastMessage = "seri")
                    )
                    contactDao.insertContacts(defaults)
                } else {
                    _uiState.update { it.copy(contacts = list) }
                }
            }
        }

        // Zamanlanmış mesajları dinle
        viewModelScope.launch {
            messageDao.getAllMessages().collect { list ->
                _uiState.update { it.copy(messages = list) }
            }
        }
    }

    fun checkApps(context: Context) {
        val whee = com.lo.seribudy.util.AppChecker.isWheeInstalled(context)
        val tiktok = com.lo.seribudy.util.AppChecker.isTikTokInstalled(context)
        val a11y = TikTokAccessibilityService.isServiceRunning

        _uiState.update {
            it.copy(
                isWheeInstalled = whee,
                isTikTokInstalled = tiktok,
                isAccessibilityEnabled = a11y
            )
        }
    }

    /**
     * DM listesinden bir kişiye tıklandığında zamanlama penceresini aç
     */
    fun onContactSelected(contact: StreakContact) {
        _uiState.update {
            it.copy(
                selectedContact = contact,
                showScheduleDialog = true,
                messageText = "seri 🔥",
                targetDateMillis = System.currentTimeMillis(),
                selectedHour = 1,
                selectedMinute = 0,
                selectedRepeat = RepeatType.DAILY
            )
        }
    }

    fun dismissScheduleDialog() {
        _uiState.update { it.copy(showScheduleDialog = false, selectedContact = null) }
    }

    fun updateMessageText(text: String) {
        _uiState.update { it.copy(messageText = text) }
    }

    fun updateHour(hour: Int) {
        _uiState.update { it.copy(selectedHour = hour) }
    }

    fun updateMinute(minute: Int) {
        _uiState.update { it.copy(selectedMinute = minute) }
    }

    fun updateRepeatType(repeatType: RepeatType) {
        _uiState.update { it.copy(selectedRepeat = repeatType) }
    }

    fun updateTargetDate(dateMillis: Long) {
        _uiState.update { it.copy(targetDateMillis = dateMillis) }
    }

    /**
     * Mesajı sessizce zamanla
     */
    fun saveSchedule(context: Context) {
        val state = _uiState.value
        val contact = state.selectedContact ?: return
        if (state.messageText.isBlank()) return

        viewModelScope.launch {
            val message = ScheduledMessage(
                recipientUsername = contact.username,
                recipientDisplayName = contact.displayName,
                messageText = state.messageText.trim(),
                targetDateMillis = state.targetDateMillis,
                hour = state.selectedHour,
                minute = state.selectedMinute,
                repeatType = state.selectedRepeat,
                isActive = true,
                status = MessageStatus.PENDING
            )

            val id = messageDao.insert(message)

            // Sessiz zamanlayıcı kur
            AlarmReceiver.scheduleMessage(
                context = context,
                messageId = id,
                recipientUser = message.recipientUsername,
                recipientName = message.recipientDisplayName,
                messageText = message.messageText,
                targetDateMillis = message.targetDateMillis,
                hour = message.hour,
                minute = message.minute
            )

            _uiState.update { it.copy(showScheduleDialog = false, selectedContact = null) }
        }
    }

    fun toggleMessage(context: Context, message: ScheduledMessage) {
        viewModelScope.launch {
            val newActive = !message.isActive
            messageDao.setActive(message.id, newActive)

            if (newActive) {
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
            } else {
                AlarmReceiver.cancelSchedule(context, message.id)
            }
        }
    }

    fun deleteMessage(context: Context, message: ScheduledMessage) {
        viewModelScope.launch {
            AlarmReceiver.cancelSchedule(context, message.id)
            messageDao.delete(message)
        }
    }

    /**
     * Arka planda anında test gönderimi yap
     */
    fun sendNowSilently(message: ScheduledMessage) {
        TikTokAccessibilityService.sendMessageSilently(
            recipientUser = message.recipientUsername,
            recipientName = message.recipientDisplayName,
            messageText = message.messageText,
            messageId = message.id
        )
    }

    /**
     * TikTok DM ekranından yeni serileri çek
     */
    fun syncContactsNow() {
        TikTokAccessibilityService.scanChats()
    }
}
