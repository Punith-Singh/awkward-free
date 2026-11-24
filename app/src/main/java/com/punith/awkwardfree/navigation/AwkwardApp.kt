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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.core.content.ContextCompat
import com.punith.awkwardfree.reminders.scheduleReminders
import com.punith.awkwardfree.reminders.scheduleFollowUp
import com.punith.awkwardfree.reminders.NotificationTarget
import com.punith.awkwardfree.reminders.resolveNotificationTarget
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable internal fun AwkwardApp(store: Store, auth: FirebaseAuth?, accountEmail: String?, quickAddPending: Boolean = false, onQuickAddHandled: () -> Unit = {}, notificationTarget: NotificationTarget? = null, onNotificationHandled: () -> Unit = {}) {
    var dues by remember(store) {
        val saved = store.read()
        val current = materializeMonthly(saved, java.time.LocalDate.now())
        if (current != saved) store.write(current)
        mutableStateOf(current)
    }
    var upi by remember { mutableStateOf(store.upi) }
    var name by remember { mutableStateOf(store.displayName) }
    var screen by remember { mutableStateOf("home") }
    var detailOrigin by remember { mutableStateOf("home") }
    var addOrigin by remember { mutableStateOf("home") }
    var friendReferenceId by remember { mutableStateOf<String?>(null) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var splitGroupId by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf(false) }
    var duplicateOf by remember { mutableStateOf<Due?>(null) }
    var returnToNudge by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(store) { if (store.remindersEnabled) scheduleReminders(context, true) }
    DisposableEffect(lifecycleOwner, store) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val saved = store.read()
                val refreshed = materializeMonthly(saved, java.time.LocalDate.now())
                if (refreshed != saved) store.write(refreshed)
                dues = refreshed
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    var reminderEnabled by remember { mutableStateOf(store.remindersEnabled) }
    var quietHoursEnabled by remember { mutableStateOf(store.quietHoursEnabled) }
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
    fun update(items: List<Due>) { val current = materializeMonthly(items, java.time.LocalDate.now()); dues = current; store.write(current) }
    fun updateWithUndo(items: List<Due>, label: String) {
        val previous = dues
        val current = materializeMonthly(items, java.time.LocalDate.now())
        val undo = DueUndo.between(previous, current)
        dues = current
        store.write(current)
        if (undo != null) {
            snackbarHost.currentSnackbarData?.dismiss()
            scope.launch {
                val result = snackbarHost.showSnackbar(label, actionLabel = "Undo", duration = SnackbarDuration.Long)
                if (result == SnackbarResult.ActionPerformed) {
                    val restored = undo.restore(dues)
                    if (restored != null) update(restored)
                    else snackbarHost.showSnackbar("Couldn’t undo because a tab changed.")
                }
            }
        }
    }
    fun navigate(target: String) { splitGroupId = null; duplicateOf = null; editing = false; returnToNudge = false; screen = target }
    fun back() { screen = when (screen) { "details" -> detailOrigin; "nudge" -> "details"; "add" -> if (editing && selected != null) "details" else addOrigin; "split" -> "add"; "friendDetails" -> "friends"; "splitGroup" -> "friends"; "settings" -> if (returnToNudge && selected != null) "nudge" else "home"; else -> "home" }; duplicateOf = null; editing = false; returnToNudge = false }
    LaunchedEffect(quickAddPending) {
        if (quickAddPending) {
            navigate("add")
            onQuickAddHandled()
        }
    }
    LaunchedEffect(notificationTarget) {
        val target = notificationTarget
        if (target != null) {
            val resolved = resolveNotificationTarget(target, auth?.currentUser?.uid ?: "", dues)
            if (resolved != null) {
                duplicateOf = null
                editing = false
                returnToNudge = false
                selectedId = resolved.first
                detailOrigin = "home"
                screen = resolved.second
            }
            onNotificationHandled()
        }
    }
    BackHandler(enabled = screen != "home") { back() }
    Scaffold(containerColor = Background, snackbarHost = { SnackbarHost(snackbarHost) }, bottomBar = {
        BottomBar(if (screen == "details" || screen == "nudge") detailOrigin else screen,
            onSelect = { navigate(it) },
            onAdd = { addOrigin = if (screen == "friends" || screen == "friendDetails" || screen == "splitGroup") "friends" else "home"; navigate("add") })
    }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (if (screen == "details" || screen == "nudge") detailOrigin else screen) {
                "home" -> DuesDashboard(dues, { navigate("friends") }, { addOrigin = "home"; navigate("add") }, { d -> detailOrigin = "home"; selectedId = d.id; screen = "details" }, { d -> detailOrigin = "home"; selectedId = d.id; screen = "nudge" })
                "friends" -> FriendsScreen(dues, upi, { d -> friendReferenceId = d.id; screen = "friendDetails" }, { d -> detailOrigin = "friends"; selectedId = d.id; screen = "nudge" }, { id -> splitGroupId = id; screen = "splitGroup" }, { reference -> updateWithUndo(offsetFriendTabs(reference, dues), "Tabs offset") }, { ids -> val opened = System.currentTimeMillis(); update(dues.map { if (it.id in ids && !it.settled && !it.owedByMe) it.recordNudgeOpened(opened) else it }) })
                "friendDetails" -> {
                    val reference = dues.firstOrNull { it.id == friendReferenceId }
                    val number = reference?.let { normalizePhone(it.phone) }
                    val friendDues = if (reference == null) emptyList() else dues.filter { due ->
                        !due.settled && if (number == null) due.id == reference.id else normalizePhone(due.phone) == number && due.name.trim().equals(reference.name.trim(), ignoreCase = true)
                    }
                    ListScreen("Friend’s tabs", "Open tabs for this friend.", friendDues, false, { screen = "friends" }, { d -> detailOrigin = "friendDetails"; selectedId = d.id; screen = "details" }, { d -> detailOrigin = "friendDetails"; selectedId = d.id; screen = "nudge" }, upi, { ids -> val opened = System.currentTimeMillis(); update(dues.map { if (it.id in ids && !it.settled && !it.owedByMe) it.recordNudgeOpened(opened) else it }) }, { d -> updateWithUndo(offsetFriendTabs(d, dues), "Tabs offset") })
                }
                "split" -> SplitDueScreen(dues, { back() }) { entries -> update(entries + dues); navigate("home") }
                "splitGroup" -> SplitGroupScreen(dues.filter { it.splitGroupId == splitGroupId }, { splitGroupId = null; screen = "friends" }, { d -> detailOrigin = "splitGroup"; selectedId = d.id; screen = "details" }, { d -> detailOrigin = "splitGroup"; selectedId = d.id; screen = "nudge" })
                "dues" -> DuesScreen(dues.filterNot { it.settled }, { back() }, { d -> selectedId = d.id; screen = "details" }, { d -> selectedId = d.id; screen = "nudge" }, upi, { ids -> val opened = System.currentTimeMillis(); update(dues.map { if (it.id in ids && !it.settled && !it.owedByMe) it.recordNudgeOpened(opened) else it }) }, { reference -> updateWithUndo(offsetFriendTabs(reference, dues), "Tabs offset") })
                "settled" -> SettledScreen(dues.filter { it.settled }, { back() }, { d -> selectedId = d.id; screen = "details" })
                "add" -> AddScreen(if (editing) selected else duplicateOf, { back() }, duplicateOf != null, { screen = "split" }) { due -> update(if (editing) dues.map { if (it.id == due.id) due else it } else listOf(due) + dues); selectedId = due.id; if (!editing) detailOrigin = addOrigin; editing = false; duplicateOf = null; screen = "details" }
                "account" -> AccountScreen(auth, accountEmail) { screen = "settings" }
                "settings" -> SettingsScreen(upi, name, reminderEnabled, backupStatus, accountEmail, { screen = "account" }, { back() }, { enabled ->
                    if (enabled && Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else { reminderEnabled = enabled; store.remindersEnabled = enabled; scheduleReminders(context, enabled); backupStatus = if (enabled) "Daily reminders enabled." else "Reminders turned off." }
                }, { exportLauncher.launch("awkward-free-backup.json") }, { importLauncher.launch(arrayOf("application/json", "text/plain")) }, store.automaticSnapshotHealthy, quietHoursEnabled, { enabled -> quietHoursEnabled = enabled; store.quietHoursEnabled = enabled }) { newUpi, newName -> upi = newUpi; name = newName; store.upi = newUpi; store.displayName = newName; screen = if (returnToNudge && selected != null) "nudge" else "home"; returnToNudge = false }
            }
        }
    }
    if (screen == "details") {
        ModalBottomSheet(onDismissRequest = { back() }) {
            Box(Modifier.fillMaxWidth()) {
            if (selected != null) DetailScreen(selected, { back() }, { editing = true; screen = "add" }, { selectedId = selected.id; screen = "nudge" }, { val items = dues.map { if (it.id == selected.id) it.copy(settled = !it.settled, paidAmount = if (it.settled && it.remaining().signum() == 0) "0.00" else it.paidAmount) else it }; if (!selected.settled) updateWithUndo(items, "Tab settled") else update(items) }, { updateWithUndo(dues.filterNot { it.id == selected.id }, "Tab deleted"); selectedId = null; screen = "home" }, { payment -> val items = dues.map { if (it.id == selected.id) it.recordPayment(payment) else it }; if (payment.compareTo(selected.remaining()) == 0) updateWithUndo(items, "Tab settled") else update(items) }, { duplicateOf = selected.copy(id = java.util.UUID.randomUUID().toString(), date = java.time.LocalDate.now().toString(), settled = false, lastNudged = 0L, nudgeHistory = emptyList(), paidAmount = "0.00", repeatMonthly = false, recurrenceThrough = "", splitGroupId = ""); editing = false; screen = "add" }, reminderEnabled, {
                scheduleFollowUp(context, auth?.currentUser?.uid ?: "", selected.id, selected.lastNudged)
                scope.launch { snackbarHost.showSnackbar("Follow-up snoozed for 3 days") }
            }) else EmptyFallback { screen = "home" }
            SnackbarHost(snackbarHost, Modifier.align(Alignment.BottomCenter).padding(12.dp))
            }
        }
    }
    if (screen == "nudge") {
        ModalBottomSheet(onDismissRequest = { back() }) {
            if (selected != null && !selected.owedByMe) NudgeScreen(selected, upi, { back() }, { returnToNudge = true; screen = "settings" }, reminderEnabled) { followUp -> val opened = System.currentTimeMillis(); update(dues.map { if (it.id == selected.id) it.recordNudgeOpened(opened) else it }); if (followUp) scheduleFollowUp(context, auth?.currentUser?.uid ?: "", selected.id, opened); screen = "details" } else EmptyFallback { screen = "home" }
        }
    }
    pendingBackup?.let { backup -> AlertDialog(onDismissRequest = { pendingBackup = null }, title = { Text("Restore backup?") }, text = { Text("This replaces your current dues, name and UPI ID with ${backup.dues.size} records from the backup. This cannot be undone unless you save a backup first.") }, confirmButton = { TextButton(onClick = { if (store.restore(backup)) { dues = materializeMonthly(backup.dues, java.time.LocalDate.now()); store.write(dues); upi = backup.upi; name = backup.displayName; selectedId = null; backupStatus = "Backup restored."; screen = "home" } else backupStatus = "Restore failed. Please try again."; pendingBackup = null }) { Text("Replace and restore") } }, dismissButton = { TextButton(onClick = { pendingBackup = null }) { Text("Cancel") } }) }
}
@Composable internal fun BottomBar(screen: String, onSelect: (String) -> Unit, onAdd: () -> Unit) {
    val active = when (screen) {
        "friends", "friendDetails", "splitGroup" -> "friends"
        "settings", "account" -> "settings"
        "add", "split" -> "add"
        else -> "home"
    }
    val colors = NavigationBarItemDefaults.colors(
        selectedIconColor = Violet, unselectedIconColor = Muted,
        indicatorColor = Lavender
    )
    val glassShape = RoundedCornerShape(28.dp)
    Box(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp, top = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        NavigationBar(
            modifier = Modifier.fillMaxWidth().height(68.dp)
                .shadow(elevation = 12.dp, shape = glassShape, ambientColor = Deep.copy(alpha = 0.16f), spotColor = Deep.copy(alpha = 0.18f))
                .background(
                    brush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.94f), Color(0xFFEAF4ED).copy(alpha = 0.90f))),
                    shape = glassShape
                )
                .border(1.dp, Color.White.copy(alpha = 0.85f), glassShape),
            containerColor = Color.Transparent,
            contentColor = Ink,
            tonalElevation = 0.dp,
            windowInsets = WindowInsets(0, 0, 0, 0)
        ) {
            NavigationBarItem(selected = active == "home", onClick = { onSelect("home") }, icon = { Icon(Icons.Outlined.Home, contentDescription = "Dues") }, colors = colors)
            NavigationBarItem(selected = active == "friends", onClick = { onSelect("friends") }, icon = { Icon(Icons.Outlined.People, contentDescription = "Friends") }, colors = colors)
            NavigationBarItem(selected = active == "add", onClick = onAdd, icon = { Icon(Icons.Outlined.AddBox, contentDescription = "Add a due") }, colors = colors)
            NavigationBarItem(selected = active == "settings", onClick = { onSelect("settings") }, icon = { Icon(Icons.Outlined.Settings, contentDescription = "Settings") }, colors = colors)
        }
    }
}
