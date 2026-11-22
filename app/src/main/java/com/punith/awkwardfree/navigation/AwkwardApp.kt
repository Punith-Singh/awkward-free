package com.punith.awkwardfree.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.punith.awkwardfree.reminders.scheduleReminders
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.punith.awkwardfree.data.*
import com.punith.awkwardfree.model.*
import com.punith.awkwardfree.ui.components.*
import com.punith.awkwardfree.ui.screens.*
import com.punith.awkwardfree.ui.theme.*

@Composable internal fun AwkwardApp(store: Store, auth: FirebaseAuth?, accountEmail: String?) {
    var dues by remember { mutableStateOf(store.read()) }
    var upi by remember { mutableStateOf(store.upi) }
    var name by remember { mutableStateOf(store.displayName) }
    var screen by remember { mutableStateOf("home") }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf(false) }
    var duplicateOf by remember { mutableStateOf<Due?>(null) }
    var returnToNudge by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var reminderEnabled by remember { mutableStateOf(store.remindersEnabled) }
    var backupStatus by remember { mutableStateOf("") }
    var pendingBackup by remember { mutableStateOf<Backup?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        reminderEnabled = granted; store.remindersEnabled = granted; scheduleReminders(context, granted)
        backupStatus = if (granted) "Daily reminders enabled." else "Notification permission was not granted."
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) try { context.contentResolver.openOutputStream(uri)?.use { it.write(store.exportBackup().toByteArray(Charsets.UTF_8)) } ?: error("Cannot open file"); backupStatus = "Backup saved." } catch (_: Exception) { backupStatus = "Couldn't save backup. Please try again." }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) try { val data = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { reader -> val chars = CharArray(2_000_001); var size = 0; while (size < chars.size) { val n = reader.read(chars, size, chars.size - size); if (n < 0) break; size += n }; require(size <= 2_000_000); String(chars, 0, size) } ?: error("Cannot open file"); pendingBackup = store.parseBackup(data); backupStatus = "" } catch (_: Exception) { backupStatus = "This backup couldn't be read. Your current data is unchanged." }
    }
    val selected = dues.firstOrNull { it.id == selectedId }
    fun update(items: List<Due>) { dues = items; store.write(items) }
    fun navigate(target: String) { duplicateOf = null; editing = false; returnToNudge = false; screen = target }
    fun back() { screen = when (screen) { "details" -> if (selected?.settled == true) "settled" else "dues"; "nudge" -> "details"; "add" -> if (editing && selected != null) "details" else "home"; "settings" -> if (returnToNudge && selected != null) "nudge" else "home"; else -> "home" }; duplicateOf = null; editing = false; returnToNudge = false }
    BackHandler(enabled = screen != "home") { back() }
    Scaffold(containerColor = Background, bottomBar = { BottomBar(screen) { navigate(it) } }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (screen) {
                "home" -> HomeScreen(dues, name, { navigate("dues") }, { navigate("settled") }, { navigate("add") }, { navigate("split") }, { d -> selectedId = d.id; screen = "details" }, { d -> selectedId = d.id; screen = "nudge" })
                "split" -> SplitDueScreen({ back() }) { entries -> update(entries + dues); navigate("dues") }
                "dues" -> DuesScreen(dues.filterNot { it.settled }, { back() }, { d -> selectedId = d.id; screen = "details" }, { d -> selectedId = d.id; screen = "nudge" }, upi, { ids -> val opened = System.currentTimeMillis(); update(dues.map { if (it.id in ids) it.copy(lastNudged = opened) else it }) })
                "settled" -> SettledScreen(dues.filter { it.settled }, { back() }, { d -> selectedId = d.id; screen = "details" })
                "add" -> AddScreen(if (editing) selected else duplicateOf, { back() }, duplicateOf != null) { due -> update(if (editing) dues.map { if (it.id == due.id) due else it } else listOf(due) + dues); selectedId = due.id; editing = false; duplicateOf = null; screen = "details" }
                "details" -> if (selected != null) DetailScreen(selected, { back() }, { editing = true; screen = "add" }, { selectedId = selected.id; screen = "nudge" }, { update(dues.map { if (it.id == selected.id) it.copy(settled = !it.settled, paidAmount = if (it.settled && it.remaining().signum() == 0) "0.00" else it.paidAmount) else it }) }, { update(dues.filterNot { it.id == selected.id }); selectedId = null; screen = "home" }, { payment -> update(dues.map { if (it.id == selected.id) it.recordPayment(payment) else it }) }, { duplicateOf = selected.copy(id = java.util.UUID.randomUUID().toString(), date = java.time.LocalDate.now().toString(), settled = false, lastNudged = 0L, paidAmount = "0.00"); editing = false; screen = "add" }) else EmptyFallback { screen = "home" }
                "nudge" -> if (selected != null && !selected.owedByMe) NudgeScreen(selected, upi, { back() }, { returnToNudge = true; screen = "settings" }) { update(dues.map { if (it.id == selected.id) it.copy(lastNudged = System.currentTimeMillis()) else it }); screen = "details" } else EmptyFallback { screen = "home" }
                "account" -> AccountScreen(auth, accountEmail) { screen = "settings" }
                "settings" -> SettingsScreen(upi, name, reminderEnabled, backupStatus, accountEmail, { screen = "account" }, { back() }, { enabled ->
                    if (enabled && Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else { reminderEnabled = enabled; store.remindersEnabled = enabled; scheduleReminders(context, enabled); backupStatus = if (enabled) "Daily reminders enabled." else "Reminders turned off." }
                }, { exportLauncher.launch("awkward-free-backup.json") }, { importLauncher.launch(arrayOf("application/json", "text/plain")) }) { newUpi, newName -> upi = newUpi; name = newName; store.upi = newUpi; store.displayName = newName; screen = if (returnToNudge && selected != null) "nudge" else "home"; returnToNudge = false }
            }
        }
    }
    pendingBackup?.let { backup -> AlertDialog(onDismissRequest = { pendingBackup = null }, title = { Text("Restore backup?") }, text = { Text("This replaces your current dues, name and UPI ID with ${backup.dues.size} records from the backup. This cannot be undone unless you save a backup first.") }, confirmButton = { TextButton(onClick = { if (store.restore(backup)) { dues = backup.dues; upi = backup.upi; name = backup.displayName; selectedId = null; backupStatus = "Backup restored."; screen = "home" } else backupStatus = "Restore failed. Please try again."; pendingBackup = null }) { Text("Replace and restore") } }, dismissButton = { TextButton(onClick = { pendingBackup = null }) { Text("Cancel") } }) }
}
@Composable internal fun BottomBar(screen: String, onSelect: (String) -> Unit) {
    val active = when (screen) { "details", "nudge" -> "dues"; "split" -> "add"; else -> screen }
    val options = listOf(Triple("home", "Home", Icons.Outlined.Home), Triple("dues", "All dues", Icons.Outlined.ListAlt), Triple("add", "Add a due", Icons.Outlined.AddBox), Triple("settled", "Settled", Icons.Outlined.CheckCircle), Triple("settings", "Settings", Icons.Outlined.AccountCircle))
    val shape = RoundedCornerShape(28.dp)
    Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 20.dp, end = 20.dp, bottom = 12.dp, top = 6.dp), contentAlignment = Alignment.Center) {
        Row(
            Modifier.fillMaxWidth().shadow(12.dp, shape, ambientColor = Violet.copy(alpha = 0.18f), spotColor = Violet.copy(alpha = 0.18f))
                .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.94f), Color(0xFFECE7FF).copy(alpha = 0.88f))), shape)
                .border(1.dp, Color.White.copy(alpha = 0.9f), shape).height(68.dp).padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEach { (key, label, icon) ->
                val selected = active == key
                val indicatorColor by animateColorAsState(if (selected) Violet.copy(alpha = 0.13f) else Color.Transparent, label = "navigation selection")
                val iconColor by animateColorAsState(if (selected) Violet else Color(0xFF79748F), label = "navigation icon")
                val iconScale by animateFloatAsState(if (selected) 1.08f else 1f, animationSpec = spring(), label = "navigation icon scale")
                IconButton(onClick = { onSelect(key) }, modifier = Modifier.semantics { this.selected = selected }) {
                    Box(Modifier.size(44.dp).background(indicatorColor, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = label, tint = iconColor, modifier = Modifier.size(24.dp).scale(iconScale))
                    }
                }
            }
        }
    }
}
