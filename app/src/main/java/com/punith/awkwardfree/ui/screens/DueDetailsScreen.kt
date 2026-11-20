package com.punith.awkwardfree.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.punith.awkwardfree.model.*
import com.punith.awkwardfree.ui.components.*
import com.punith.awkwardfree.ui.theme.*
import com.punith.awkwardfree.util.*

@Composable internal fun DetailScreen(d: Due, back: () -> Unit, edit: () -> Unit, nudge: () -> Unit, toggle: () -> Unit, delete: () -> Unit) {
    var confirmDelete by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        BackHeader(if (d.settled) "Settled" else "Open tab", back)
        Heading("The little details", "${d.name}’s tab.", d.reason)
        Column(Modifier.fillMaxWidth().background(Deep, RoundedCornerShape(24.dp)).padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Pill(if (d.settled) "SETTLED" else "OPEN TAB", Color(0xFF4D427C), Color(0xFFE4DFFF)); Text("✦", color = Lime, fontSize = 21.sp) }
            Text("AMOUNT OWED", color = Color(0xFFD2C9FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(money(d.amount), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 38.sp)
            Text("Due ${dateLabel(d.date)}", color = Color(0xFFDCD5FF), fontSize = 13.sp)
        }
        Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(20.dp)).padding(17.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("TAB ACTIVITY", color = Muted, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(44.dp).background(Lavender, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Schedule, null, tint = Violet) }; Spacer(Modifier.width(12.dp)); Column { Text(if (d.lastNudged == 0L) "No nudges yet" else nudgeLabel(d.lastNudged), color = Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp); Text(if (d.lastNudged == 0L) "A friendly reminder is a good start." else "WhatsApp was opened with your message.", color = Muted, fontSize = 12.sp) } }
        }
        if (!d.settled) PrimaryButton("Send a nudge", nudge)
        OutlinedButton(onClick = toggle, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(17.dp), colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = Ink), border = null) { Text(if (d.settled) "Reopen tab" else "✓   Mark as settled", fontWeight = FontWeight.Bold) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { TextButton(onClick = edit) { Text("Edit details") }; TextButton(onClick = { confirmDelete = true }) { Text("Delete tab", color = Muted) } }
        Text("Once you’ve confirmed the payment, mark this tab as settled.", color = Muted, fontSize = 12.sp)
    }
    if (confirmDelete) AlertDialog(onDismissRequest = { confirmDelete = false }, title = { Text("Delete this tab?") }, text = { Text("This removes ${d.name}'s ${money(d.amount)} entry from this device.") }, confirmButton = { TextButton(onClick = { confirmDelete = false; delete() }) { Text("Delete") } }, dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } })
}
