package com.punith.awkwardfree.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.punith.awkwardfree.ui.components.*
import com.punith.awkwardfree.ui.theme.*
import com.punith.awkwardfree.util.*

@Composable internal fun SettingsScreen(currentUpi: String, currentName: String, remindersEnabled: Boolean, backupStatus: String, back: () -> Unit, setReminders: (Boolean) -> Unit, exportBackup: () -> Unit, importBackup: () -> Unit, save: (String, String) -> Unit) {
    var upi by remember { mutableStateOf(currentUpi) }; var name by remember { mutableStateOf(currentName) }; var error by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        BackHeader("Your settings", back)
        Heading("Make it yours", "Payment settings.", "Make it easy for friends to pay.")
        Column(Modifier.fillMaxWidth().background(Deep, RoundedCornerShape(22.dp)).padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("YOUR UPI DETAILS", color = Color(0xFFD2C9FF), fontSize = 11.sp, fontWeight = FontWeight.Bold); Text("Where should they pay you?", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold); Text("Your UPI ID is included in your payment link and message.", color = Color(0xFFDCD5FF), fontSize = 13.sp) }
        Input(name, { name = it }, "Your name (optional)", "Punith")
        Input(upi, { upi = it }, "Your UPI ID", "yourname@upi")
        Text("Double-check your UPI ID before sharing. Your data stays on this device.", color = Muted, fontSize = 12.sp)
        if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error)
        PrimaryButton("Save settings", { val value = upi.trim(); if (value.isNotEmpty() && !value.matches(Regex("[A-Za-z0-9._-]+@[A-Za-z0-9._-]+"))) error = "Enter a valid UPI ID, like name@bank." else save(value, name.trim()) })
        Row(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(20.dp)).padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text("Due-date reminders", fontWeight = FontWeight.Bold, color = Ink); Text("Daily around 9 AM for tabs due tomorrow, today or overdue. Timing can vary.", fontSize = 12.sp, color = Muted) }
            Switch(checked = remindersEnabled, onCheckedChange = setReminders)
        }
        Text("BACKUP & RESTORE", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text("A backup contains names, phone numbers, dues and your UPI ID. Keep the file somewhere private. Only saved settings are included.", color = Muted, fontSize = 12.sp)
        OutlinedButton(onClick = exportBackup, modifier = Modifier.fillMaxWidth()) { Text("Export backup") }
        OutlinedButton(onClick = importBackup, modifier = Modifier.fillMaxWidth()) { Text("Restore backup") }
        if (backupStatus.isNotBlank()) Text(backupStatus, color = Violet, fontSize = 13.sp)
        EmptyCard("Your data, your control", "Dues are stored only on this device. Confirm payments yourself before marking them settled.")
    }
}
