package com.punith.awkwardfree.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.punith.awkwardfree.ui.components.*
import com.punith.awkwardfree.ui.theme.*

private val UpiPattern = Regex("[A-Za-z0-9._-]+@[A-Za-z0-9._-]+")
private val SoftLavender = Color(0xFFD2C9FF)

@Composable
internal fun SettingsScreen(
    currentUpi: String,
    currentName: String,
    remindersEnabled: Boolean,
    backupStatus: String,
    accountEmail: String?,
    openAccount: () -> Unit,
    back: () -> Unit,
    setReminders: (Boolean) -> Unit,
    exportBackup: () -> Unit,
    importBackup: () -> Unit,
    save: (String, String) -> Unit
) {
    val context = LocalContext.current
    var upi by remember { mutableStateOf(currentUpi) }
    var name by remember { mutableStateOf(currentName) }
    val trimmedUpi = upi.trim()
    val trimmedName = name.trim()
    val upiInvalid = trimmedUpi.isNotEmpty() && !trimmedUpi.matches(UpiPattern)
    val changed = trimmedUpi != currentUpi.trim() || trimmedName != currentName.trim()
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull().orEmpty()
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        BackHeader("Settings", back)
        Heading("Make it yours", "Your settings.", "Payment details, reminders and your data, all in one place.")

        ProfileCard(trimmedName, accountEmail, openAccount)

        SectionLabel("PAYMENTS")
        Input(name, { name = it }, "Your name (optional)", "How friends see you")
        Input(upi, { upi = it }, "Your UPI ID", "yourname@upi")
        if (upiInvalid) {
            Text("Enter a valid UPI ID, like name@bank.", color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp))
        } else {
            Text(
                "Included in your payment link and reminder messages. Double-check it before sharing.",
                color = Muted, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(start = 4.dp)
            )
        }
        Button(
            onClick = { save(trimmedUpi, trimmedName) },
            enabled = changed && !upiInvalid,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(17.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Violet, disabledContainerColor = Line, disabledContentColor = Muted)
        ) { Text("Save changes", fontWeight = FontWeight.Bold, fontSize = 14.sp) }

        SectionLabel("NOTIFICATIONS")
        SettingsGroup {
            SettingsRow(
                icon = Icons.Outlined.Notifications,
                title = "Due-date reminders",
                subtitle = "Daily around 9 AM for tabs due tomorrow, today or overdue. Timing can vary.",
                modifier = Modifier.toggleable(value = remindersEnabled, role = Role.Switch, onValueChange = setReminders)
            ) {
                Switch(
                    checked = remindersEnabled,
                    onCheckedChange = null,
                    colors = SwitchDefaults.colors(checkedTrackColor = Violet, checkedThumbColor = Color.White)
                )
            }
        }

        SectionLabel("BACKUP & RESTORE")
        SettingsGroup {
            SettingsRow(
                icon = Icons.Outlined.FileUpload,
                title = "Export backup",
                subtitle = "Save a copy of your dues, name and UPI ID.",
                modifier = Modifier.clickable(role = Role.Button, onClick = exportBackup)
            ) { Chevron() }
            RowDivider()
            SettingsRow(
                icon = Icons.Outlined.FileDownload,
                title = "Restore backup",
                subtitle = "Replace current data with a saved backup file.",
                modifier = Modifier.clickable(role = Role.Button, onClick = importBackup)
            ) { Chevron() }
        }
        if (backupStatus.isNotBlank()) {
            Row(
                Modifier.fillMaxWidth().background(Lavender, RoundedCornerShape(14.dp)).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Outlined.Info, contentDescription = null, tint = Violet, modifier = Modifier.size(18.dp))
                Text(backupStatus, color = Violet, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            }
        }
        Text(
            "A backup contains names, phone numbers, dues and your UPI ID. Keep the file somewhere private. Only saved settings are included.",
            color = Muted, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(start = 4.dp)
        )

        SectionLabel("ABOUT")
        SettingsGroup {
            SettingsRow(
                icon = Icons.Outlined.Lock,
                title = "Your data, your control",
                subtitle = "Dues are stored only on this device. Confirm payments yourself before marking them settled."
            )
            if (version.isNotBlank()) {
                RowDivider()
                SettingsRow(icon = Icons.Outlined.Info, title = "Version", subtitle = version)
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ProfileCard(name: String, accountEmail: String?, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Deep)
            .clickable(role = Role.Button, onClick = onClick).padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(Modifier.size(52.dp).background(Lime, CircleShape), contentAlignment = Alignment.Center) {
            Text(name.take(1).uppercase().ifEmpty { "A" }, color = Deep, fontWeight = FontWeight.ExtraBold, fontSize = 21.sp)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                name.ifEmpty { "Your profile" }, color = Color.White, fontWeight = FontWeight.ExtraBold,
                fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                accountEmail ?: "Sign in or create an account", color = SoftLavender, fontSize = 12.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        Icon(Icons.Outlined.ChevronRight, contentDescription = "Open account", tint = SoftLavender)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text, color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = .6.sp,
        modifier = Modifier.padding(start = 4.dp, top = 6.dp)
    )
}

@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color.White), content = content)
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {}
) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(Modifier.size(40.dp).background(Lavender, RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = Violet, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(subtitle, color = Muted, fontSize = 12.sp, lineHeight = 17.sp)
        }
        trailing()
    }
}

@Composable
private fun Chevron() {
    Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = Muted)
}

@Composable
private fun RowDivider() {
    HorizontalDivider(color = Line, modifier = Modifier.padding(start = 70.dp))
}
