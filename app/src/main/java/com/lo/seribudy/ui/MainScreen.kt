package com.lo.seribudy.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lo.seribudy.data.RepeatType
import com.lo.seribudy.data.ScheduledMessage
import com.lo.seribudy.data.StreakContact
import com.lo.seribudy.ui.theme.TikTokCyan
import com.lo.seribudy.ui.theme.TikTokRed
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel.checkApps(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🔥 Streaker", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                },
                actions = {
                    // DM'leri tara/eşitle butonu
                    IconButton(onClick = { viewModel.syncContactsNow() }) {
                        Icon(Icons.Default.Sync, contentDescription = "Eşitle", tint = TikTokCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Durum Göstergeleri
            StatusSection(uiState = uiState, context = context)

            // Sekmeler: 1. DM Sohbetleri, 2. Zamanlananlar
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = TikTokCyan
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("💬 Seri Sohbetleri (${uiState.contacts.size})", fontWeight = FontWeight.SemiBold)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⏰ Zamanlananlar (${uiState.messages.size})", fontWeight = FontWeight.SemiBold)
                        }
                    }
                )
            }

            // Sekme İçerikleri
            if (selectedTab == 0) {
                // DM Listesi - Kullanıcı tek tıkla seri dostunu seçer
                DmContactsList(
                    contacts = uiState.contacts,
                    onContactClick = { contact ->
                        viewModel.onContactSelected(contact)
                    }
                )
            } else {
                // Zamanlanmış Mesajlar Listesi
                ScheduledList(
                    messages = uiState.messages,
                    onToggle = { msg -> viewModel.toggleMessage(context, msg) },
                    onDelete = { msg -> viewModel.deleteMessage(context, msg) },
                    onSendNow = { msg -> viewModel.sendNowSilently(msg) }
                )
            }
        }

        // Seçilen kişi için mesaj zamanlama penceresi
        if (uiState.showScheduleDialog && uiState.selectedContact != null) {
            ScheduleBottomSheet(
                contact = uiState.selectedContact!!,
                messageText = uiState.messageText,
                targetDateMillis = uiState.targetDateMillis,
                hour = uiState.selectedHour,
                minute = uiState.selectedMinute,
                repeatType = uiState.selectedRepeat,
                onMessageChange = viewModel::updateMessageText,
                onHourChange = viewModel::updateHour,
                onMinuteChange = viewModel::updateMinute,
                onRepeatChange = viewModel::updateRepeatType,
                onDateChange = viewModel::updateTargetDate,
                onConfirm = { viewModel.saveSchedule(context) },
                onDismiss = viewModel::dismissScheduleDialog
            )
        }
    }
}

@Composable
fun StatusSection(uiState: MainUiState, context: android.content.Context) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatusPill("Whee", uiState.isWheeInstalled, Modifier.weight(1f))
            StatusPill("TikTok", uiState.isTikTokInstalled, Modifier.weight(1f))
            StatusPill("Sessiz Servis", uiState.isAccessibilityEnabled, Modifier.weight(1f))
        }

        if (!uiState.isAccessibilityEnabled) {
            Spacer(modifier = Modifier.height(6.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = TikTokRed.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Arkada sessizce gönderebilmesi için erişilebilirlik iznini açın.",
                        fontSize = 12.sp,
                        color = TikTokRed,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    }) {
                        Text("AÇ", color = TikTokRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun StatusPill(label: String, isOk: Boolean, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = if (isOk) TikTokCyan.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (isOk) Color(0xFF00E676) else Color(0xFFFF5252))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = if (isOk) TikTokCyan else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * DM Sohbetleri Arayüzü: Tıpkı TikTok gelen kutusu gibi görünür,
 * kullanıcının elle isim yazmasına gerek kalmaz.
 */
@Composable
fun DmContactsList(
    contacts: List<StreakContact>,
    onContactClick: (StreakContact) -> Unit
) {
    if (contacts.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Sohbet bulunamadı. TikTok'u açtığınızda otomatik algılanır.", color = Color.Gray, fontSize = 14.sp)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    "Seri yaptığın kişiyi seçerek otomatik mesaj ayarla:",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            items(contacts, key = { it.username }) { contact ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onContactClick(contact) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Profil Avatarı
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(TikTokRed.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                contact.displayName.take(1).uppercase(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = TikTokRed
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    contact.displayName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (contact.hasStreak) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = TikTokRed.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            "🔥 ${contact.streakDays} gün",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TikTokRed
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                contact.lastMessage,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Mesaj Zamanla Butonu
                        FilledTonalButton(
                            onClick = { onContactClick(contact) },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Ayarla ⚡", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Zamanlanan Mesajlar Listesi
 */
@Composable
fun ScheduledList(
    messages: List<ScheduledMessage>,
    onToggle: (ScheduledMessage) -> Unit,
    onDelete: (ScheduledMessage) -> Unit,
    onSendNow: (ScheduledMessage) -> Unit
) {
    if (messages.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("😴", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Henüz zamanlanmış sessiz mesaj yok", color = Color.Gray, fontSize = 14.sp)
                Text("DM sekmesinden bir kişi seçerek kurabilirsiniz", color = Color.Gray, fontSize = 12.sp)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                val timeStr = "${msg.hour.toString().padStart(2, '0')}:${msg.minute.toString().padStart(2, '0')}"
                val dateFmt = SimpleDateFormat("dd MMM", Locale("tr")).format(Date(msg.targetDateMillis))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = TikTokCyan.copy(alpha = 0.15f)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(timeStr, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TikTokCyan)
                                Text(dateFmt, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(msg.recipientDisplayName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(
                                "Mesaj: \"${msg.messageText}\"",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Test Et
                        IconButton(onClick = { onSendNow(msg) }) {
                            Icon(Icons.Default.Send, contentDescription = "Test", tint = TikTokCyan, modifier = Modifier.size(20.dp))
                        }

                        // Aktif/Pasif
                        Switch(
                            checked = msg.isActive,
                            onCheckedChange = { onToggle(msg) }
                        )

                        // Sil
                        IconButton(onClick = { onDelete(msg) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Sil", tint = TikTokRed.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * "Kime, Şu gün, Şu saatte, Ne yazalım?" Seçim Penceresi
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleBottomSheet(
    contact: StreakContact,
    messageText: String,
    targetDateMillis: Long,
    hour: Int,
    minute: Int,
    repeatType: RepeatType,
    onMessageChange: (String) -> Unit,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    onRepeatChange: (RepeatType) -> Unit,
    onDateChange: (Long) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Başlık
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🔥", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(contact.displayName, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("için sessiz arka plan zamanlaması", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Hızlı Mesaj Şablonları & Giriş
            Text("Ne gönderelim?", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("seri 🔥", "seri", "naber", "alev alev 🔥🔥").forEach { chipText ->
                    FilterChip(
                        selected = messageText == chipText,
                        onClick = { onMessageChange(chipText) },
                        label = { Text(chipText, fontSize = 12.sp) }
                    )
                }
            }

            OutlinedTextField(
                value = messageText,
                onValueChange = onMessageChange,
                label = { Text("Mesaj Metni") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Saat Seçimi
            Text("Saat kaçta?", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Saat
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = { onHourChange((hour + 1) % 24) }) {
                        Icon(Icons.Default.KeyboardArrowUp, null)
                    }
                    Text(
                        hour.toString().padStart(2, '0'),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = TikTokRed
                    )
                    IconButton(onClick = { onHourChange(if (hour > 0) hour - 1 else 23) }) {
                        Icon(Icons.Default.KeyboardArrowDown, null)
                    }
                }

                Text(":", fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp))

                // Dakika
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = { onMinuteChange((minute + 5) % 60) }) {
                        Icon(Icons.Default.KeyboardArrowUp, null)
                    }
                    Text(
                        minute.toString().padStart(2, '0'),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = TikTokCyan
                    )
                    IconButton(onClick = { onMinuteChange(if (minute >= 5) minute - 5 else 55) }) {
                        Icon(Icons.Default.KeyboardArrowDown, null)
                    }
                }
            }

            // Hangi Gün / Tekrar
            Text("Tekrar Durumu", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RepeatType.entries.forEach { type ->
                    val label = when (type) {
                        RepeatType.ONCE -> "Bugün / 1 Kez"
                        RepeatType.DAILY -> "Her Gün Bu Saatte"
                        RepeatType.WEEKLY -> "Her Hafta"
                    }
                    FilterChip(
                        selected = repeatType == type,
                        onClick = { onRepeatChange(type) },
                        label = { Text(label, fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Onayla Butonu
            Button(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = TikTokRed),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Schedule, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Arka Planda Sessizce Gönder", fontWeight = FontWeight.Bold)
            }
        }
    }
}
