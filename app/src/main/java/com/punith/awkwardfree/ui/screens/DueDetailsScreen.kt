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
import androidx.compose.ui.text.input.KeyboardType
import java.math.BigDecimal
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.punith.awkwardfree.model.*
import com.punith.awkwardfree.ui.components.*
import com.punith.awkwardfree.ui.theme.*
import com.punith.awkwardfree.util.*

@Composable internal fun DetailScreen(d: Due, back: () -> Unit, edit: () -> Unit, nudge: () -> Unit, toggle: () -> Unit, delete: () -> Unit, recordPayment: (BigDecimal) -> Unit, duplicate: () -> Unit) {
    var confirmDelete by remember { mutableStateOf(false) }
    var showPayment by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    var payment by remember { mutableStateOf("") }
    var paymentError by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        BackHeader(if (d.settled) "Settled" else "Open tab", back)
        Heading("The little details", "${d.name}’s tab.", d.reason)
        Column(Modifier.fillMaxWidth().background(Deep, RoundedCornerShape(24.dp)).padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Pill(if (d.settled) "SETTLED" else "OPEN TAB", Color(0xFF4D427C), Color(0xFFE4DFFF)); Text("✦", color = Lime, fontSize = 21.sp) }
            Text(if (d.settled) "TOTAL TAB" else "REMAINING", color = Color(0xFFD2C9FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(money(if (d.settled) d.amount else d.remaining().toPlainString()), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 38.sp)
            if (d.paidAmount.toBigDecimalOrNull() != null && d.paidAmount.toBigDecimal() > BigDecimal.ZERO) Text("Paid ${money(d.paidAmount)} of ${money(d.amount)}", color = Color(0xFFDCD5FF), fontSize = 13.sp)
            Text("Due ${dateLabel(d.date)}", color = Color(0xFFDCD5FF), fontSize = 13.sp)
        }
        Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(20.dp)).padding(17.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (d.owedByMe) "MONEY YOU OWE" else "TAB ACTIVITY", color = Muted, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(44.dp).background(Lavender, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Schedule, null, tint = Violet) }; Spacer(Modifier.width(12.dp)); Column { Text(if (d.owedByMe) "You owe ${d.name}" else if (d.lastNudged == 0L) "No nudges yet" else nudgeLabel(d.lastNudged), color = Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp); Text(if (d.owedByMe) "Track your repayments here." else if (d.lastNudged == 0L) "A friendly reminder is a good start." else "WhatsApp was opened with your message.", color = Muted, fontSize = 12.sp) } }
        }
        if (!d.owedByMe && d.lastNudged > 0) {
            val history = if (d.nudgeHistory.isEmpty()) listOf(d.lastNudged) else d.nudgeHistory
            Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(20.dp)).padding(17.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("REMINDER ACTIVITY", color = Muted, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text("WhatsApp opened ${history.size} ${if (history.size == 1) "time" else "times"} (up to the last 100).", color = Ink, fontSize = 13.sp)
                history.asReversed().take(5).forEach { Text(nudgeLabel(it), color = Muted, fontSize = 12.sp) }
                if (!d.settled) {
                    val followUp = java.time.Instant.ofEpochMilli(d.lastNudged).atZone(java.time.ZoneId.systemDefault()).toLocalDate().plusDays(3)
                    Text(if (java.time.LocalDate.now().isBefore(followUp)) "Suggested follow-up: ${dateLabel(followUp.toString())}" else "You could follow up now.", color = Violet, fontSize = 12.sp)
                }
                Text("Opening WhatsApp does not confirm a message was sent.", color = Muted, fontSize = 12.sp)
            }
        }
        if (!d.settled && !d.owedByMe) PrimaryButton("Send a nudge", nudge)
        if (!d.settled) OutlinedButton(onClick = { showPayment = true }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(17.dp)) { Text("Record a payment", fontWeight = FontWeight.Bold) }
        OutlinedButton(onClick = { if (d.settled && d.remaining().signum() == 0) confirmReset = true else toggle() }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(17.dp), colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = Ink), border = null) { Text(if (d.settled) "Reopen tab" else "✓   Mark as settled", fontWeight = FontWeight.Bold) }
        OutlinedButton(onClick = duplicate, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(17.dp)) { Text("Duplicate this tab", fontWeight = FontWeight.Bold) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { TextButton(onClick = edit) { Text("Edit details") }; TextButton(onClick = { confirmDelete = true }) { Text("Delete tab", color = Muted) } }
        Text(if (d.owedByMe) "Record payments you have made and mark the tab settled when paid." else "Once you’ve confirmed the payment, mark this tab as settled.", color = Muted, fontSize = 12.sp)
    }
    if (showPayment) AlertDialog(onDismissRequest = { showPayment = false }, title = { Text("Record a payment") }, text = { Column { Text("Remaining: ${money(d.remaining().toPlainString())}"); Spacer(Modifier.height(12.dp)); Input(payment, { payment = it; paymentError = "" }, if (d.owedByMe) "Amount paid (₹)" else "Amount received (₹)", "0", KeyboardType.Decimal); if (paymentError.isNotEmpty()) Text(paymentError, color = MaterialTheme.colorScheme.error) } }, confirmButton = { TextButton(onClick = { val value = payment.toBigDecimalOrNull(); if (value == null || value <= BigDecimal.ZERO || value.scale() > 2 || value > d.remaining()) paymentError = "Enter an amount up to the remaining balance, with at most 2 decimals." else { recordPayment(value); showPayment = false; payment = "" } }) { Text("Record") } }, dismissButton = { TextButton(onClick = { showPayment = false }) { Text("Cancel") } })
    if (confirmReset) AlertDialog(onDismissRequest = { confirmReset = false }, title = { Text("Reopen this tab?") }, text = { Text("This resets the recorded payment total to zero so the full amount is owed again.") }, confirmButton = { TextButton(onClick = { confirmReset = false; toggle() }) { Text("Reopen and reset") } }, dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } })
    if (confirmDelete) AlertDialog(onDismissRequest = { confirmDelete = false }, title = { Text("Delete this tab?") }, text = { Text("This removes ${d.name}'s ${money(d.amount)} entry from this device.") }, confirmButton = { TextButton(onClick = { confirmDelete = false; delete() }) { Text("Delete") } }, dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } })
}
