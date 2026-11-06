package com.lo.seribudy.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.lo.seribudy.SeriBuddyApp
import com.lo.seribudy.data.MessageStatus
import com.lo.seribudy.data.StreakContact
import kotlinx.coroutines.*

/**
 * TikTok Sessiz Otomasyon & Sohbet Tarama Servisi
 */
class TikTokAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var currentTask: AutomationTask? = null
    private var automationStep = AutomationStep.IDLE

    companion object {
        private const val TAG = "SeriBuddy_A11y"

        var instance: TikTokAccessibilityService? = null
            private set

        var isServiceRunning = false
            private set

        fun sendMessageSilently(
            recipientUser: String,
            recipientName: String,
            messageText: String,
            messageId: Long
        ) {
            val service = instance ?: run {
                Log.e(TAG, "Servis çalışmıyor!")
                return
            }
            service.startSilentAutomation(recipientUser, recipientName, messageText, messageId)
        }

        fun scanChats() {
            instance?.scanInboxForStreaks()
        }
    }

    data class AutomationTask(
        val recipientUser: String,
        val recipientName: String,
        val messageText: String,
        val messageId: Long
    )

    enum class AutomationStep {
        IDLE,
        OPENING_TIKTOK,
        WAITING_FOR_INBOX,
        FINDING_CHAT,
        TYPING,
        SENDING,
        RETURNING_HOME,
        DONE
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        isServiceRunning = true
        Log.d(TAG, "Sessiz otomasyon servisi aktif")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        isServiceRunning = false
        scope.cancel()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Servis sisteme bağlandı")
    }

    override fun onInterrupt() {
        resetTask()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val pkg = event.packageName?.toString() ?: ""
        if (pkg.contains("musically") || pkg.contains("trill") || pkg.contains("whee")) {
            // TikTok veya Whee ekranı açıldığında DM ve serileri otomatik tara
            scanInboxForStreaks()

            if (currentTask != null) {
                handleAutomationStep()
            }
        }
    }

    /**
     * Ekranda görünen TikTok DM sohbetlerini ve seri dostlarını tara ve veritabanına kaydet
     */
    fun scanInboxForStreaks() {
        val rootNode = rootInActiveWindow ?: return
        val allNodes = getAllNodes(rootNode)

        val foundContacts = mutableListOf<StreakContact>()

        for (node in allNodes) {
            val text = node.text?.toString() ?: ""
            val desc = node.contentDescription?.toString() ?: ""

            // Seri simgesi (🔥), "seri", "streak" veya DM öğesi kontrolü
            if (text.contains("🔥") || desc.contains("🔥") ||
                text.contains("seri", ignoreCase = true) ||
                desc.contains("seri", ignoreCase = true)
            ) {
                val cleanName = text.replace("🔥", "").trim()
                if (cleanName.isNotBlank() && cleanName.length < 30) {
                    foundContacts.add(
                        StreakContact(
                            username = cleanName,
                            displayName = cleanName,
                            hasStreak = true,
                            streakDays = 1,
                            lastMessage = "Seri dostu 🔥",
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
            }
        }

        if (foundContacts.isNotEmpty()) {
            scope.launch(Dispatchers.IO) {
                SeriBuddyApp.instance.database.contactDao().insertContacts(foundContacts)
                Log.d(TAG, "${foundContacts.size} seri sohbeti kaydedildi!")
            }
        }
    }

    /**
     * Kullanıcı fark etmeden arka planda hızlı mesaj gönderimi
     */
    private fun startSilentAutomation(
        recipientUser: String,
        recipientName: String,
        messageText: String,
        messageId: Long
    ) {
        currentTask = AutomationTask(recipientUser, recipientName, messageText, messageId)
        automationStep = AutomationStep.OPENING_TIKTOK

        val tikTokPackage = com.lo.seribudy.util.AppChecker.getTikTokPackage(this)
        if (tikTokPackage != null) {
            val intent = packageManager.getLaunchIntentForPackage(tikTokPackage)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
                startActivity(intent)
                automationStep = AutomationStep.WAITING_FOR_INBOX

                // 25 saniye zaman aşımı kontrolü
                handler.postDelayed({
                    if (automationStep != AutomationStep.DONE && automationStep != AutomationStep.IDLE) {
                        returnToHome()
                        failTask("Zaman aşımı")
                    }
                }, 25_000)
            } else {
                failTask("TikTok başlatılamadı")
            }
        } else {
            failTask("TikTok bulunamadı")
        }
    }

    private fun handleAutomationStep() {
        val rootNode = rootInActiveWindow ?: return

        when (automationStep) {
            AutomationStep.WAITING_FOR_INBOX -> {
                handler.postDelayed({
                    findAndOpenChat(rootInActiveWindow ?: return@postDelayed)
                }, 1200)
            }

            AutomationStep.FINDING_CHAT -> {
                handler.postDelayed({
                    typeAndSend(rootInActiveWindow ?: return@postDelayed)
                }, 1000)
            }

            else -> {}
        }
    }

    private fun findAndOpenChat(rootNode: AccessibilityNodeInfo) {
        val task = currentTask ?: return
        automationStep = AutomationStep.FINDING_CHAT

        // Kişiyi bulmaya çalış
        val targets = listOf(task.recipientName, task.recipientUser)
        for (name in targets) {
            val nodes = rootNode.findAccessibilityNodeInfosByText(name)
            for (node in nodes) {
                if (clickNode(node)) {
                    Log.d(TAG, "Sohbete tıklandı: $name")
                    handler.postDelayed({
                        typeAndSend(rootInActiveWindow ?: return@postDelayed)
                    }, 1200)
                    return
                }
            }
        }

        // Genel arama
        val allNodes = getAllNodes(rootNode)
        for (node in allNodes) {
            val text = node.text?.toString() ?: ""
            if (targets.any { text.contains(it, ignoreCase = true) }) {
                if (clickNode(node)) {
                    handler.postDelayed({
                        typeAndSend(rootInActiveWindow ?: return@postDelayed)
                    }, 1200)
                    return
                }
            }
        }
    }

    private fun typeAndSend(rootNode: AccessibilityNodeInfo) {
        val task = currentTask ?: return
        automationStep = AutomationStep.TYPING

        val editTexts = findEditTexts(rootNode)
        val inputField = editTexts.lastOrNull()

        if (inputField != null) {
            val args = Bundle().apply {
                putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    task.messageText
                )
            }
            inputField.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)

            handler.postDelayed({
                pressSendButton(rootInActiveWindow ?: return@postDelayed)
            }, 600)
        } else {
            returnToHome()
            failTask("Yazma alanı bulunamadı")
        }
    }

    private fun pressSendButton(rootNode: AccessibilityNodeInfo) {
        automationStep = AutomationStep.SENDING

        val sendKeywords = listOf("Gönder", "Send", "gönder", "send")
        for (kw in sendKeywords) {
            val nodes = rootNode.findAccessibilityNodeInfosByText(kw)
            for (node in nodes) {
                if (clickNode(node)) {
                    finishAndReturnHome()
                    return
                }
            }
        }

        // Content description kontrolü
        val allNodes = getAllNodes(rootNode)
        for (node in allNodes) {
            val desc = node.contentDescription?.toString()?.lowercase() ?: ""
            if (desc.contains("send") || desc.contains("gönder")) {
                if (clickNode(node)) {
                    finishAndReturnHome()
                    return
                }
            }
        }

        // Sağ alt köşe dokunma
        val metrics = resources.displayMetrics
        performTap(metrics.widthPixels * 0.9f, metrics.heightPixels * 0.85f)
        handler.postDelayed({
            finishAndReturnHome()
        }, 800)
    }

    /**
     * Mesaj atıldıktan hemen sonra kullanıcı fark etmesin diye anında masaüstüne dön
     */
    private fun finishAndReturnHome() {
        automationStep = AutomationStep.RETURNING_HOME
        val task = currentTask ?: return

        scope.launch(Dispatchers.IO) {
            val dao = SeriBuddyApp.instance.database.messageDao()
            dao.updateStatus(task.messageId, MessageStatus.SENT)
        }

        returnToHome()
        automationStep = AutomationStep.DONE
        resetTask()
    }

    private fun returnToHome() {
        performGlobalAction(GLOBAL_ACTION_HOME)
    }

    private fun failTask(reason: String) {
        val task = currentTask ?: return
        scope.launch(Dispatchers.IO) {
            val dao = SeriBuddyApp.instance.database.messageDao()
            dao.updateStatus(task.messageId, MessageStatus.FAILED)
        }
        resetTask()
    }

    private fun resetTask() {
        currentTask = null
        automationStep = AutomationStep.IDLE
        handler.removeCallbacksAndMessages(null)
    }

    private fun clickNode(node: AccessibilityNodeInfo): Boolean {
        var current: AccessibilityNodeInfo? = node
        while (current != null) {
            if (current.isClickable) {
                return current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            current = current.parent
        }
        val rect = android.graphics.Rect()
        node.getBoundsInScreen(rect)
        if (rect.width() > 0 && rect.height() > 0) {
            performTap(rect.centerX().toFloat(), rect.centerY().toFloat())
            return true
        }
        return false
    }

    private fun performTap(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 80))
            .build()
        dispatchGesture(gesture, null, null)
    }

    private fun findEditTexts(rootNode: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
        val result = mutableListOf<AccessibilityNodeInfo>()
        fun recurse(node: AccessibilityNodeInfo) {
            if (node.className?.toString() == "android.widget.EditText" || node.isEditable) {
                result.add(node)
            }
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { recurse(it) }
            }
        }
        recurse(rootNode)
        return result
    }

    private fun getAllNodes(rootNode: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
        val result = mutableListOf<AccessibilityNodeInfo>()
        fun recurse(node: AccessibilityNodeInfo) {
            result.add(node)
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { recurse(it) }
            }
        }
        recurse(rootNode)
        return result
    }
}
