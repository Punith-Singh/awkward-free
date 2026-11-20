package com.punith.awkwardfree.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.punith.awkwardfree.model.*
import com.punith.awkwardfree.ui.components.*
import com.punith.awkwardfree.ui.theme.*
import com.punith.awkwardfree.util.*

@Composable internal fun NudgeScreen(d: Due, upi: String, back: () -> Unit, settings: () -> Unit, onOpened: () -> Unit) {
    val context = LocalContext.current
    var tone by remember(d.id, upi) { mutableStateOf(if (d.lastNudged > 0) "Follow-up" else "Friendly") }; var text by remember(d.id, upi) { mutableStateOf(message(d, upi, if (d.lastNudged > 0) "Follow-up" else "Friendly")) }; var error by remember { mutableStateOf("") }; var confirmTone by remember { mutableStateOf<String?>(null) }
    fun chooseTone(option: String) { tone = option; text = message(d, upi, option) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
        BackHeader("Almost there", back)
        Heading("Let’s keep it easy", "A little nudge.", "Choose your vibe and make it yours.")
        Row(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(19.dp)).padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Avatar(d.name); Spacer(Modifier.width(12.dp)); Column { Text("${d.name} owes you", color = Muted, fontSize = 12.sp); Text("${money(d.remaining().toPlainString())}  ·  ${d.reason}", color = Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp) } }
        Text("PICK A TONE", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf("Friendly", "Playful", "Super casual", "Close friend", "Follow-up", "Polite follow-up", "Direct").forEach { option -> Pill(option, if (tone == option) Violet else Color.White, if (tone == option) Color.White else Ink) { if (text != message(d, upi, tone)) confirmTone = option else chooseTone(option) } } }
        Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(20.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("MESSAGE PREVIEW  ·  TAP TO EDIT", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            HorizontalDivider(color = Line)
            BasicTextField(value = text, onValueChange = { text = it }, modifier = Modifier.fillMaxWidth().heightIn(min = 90.dp), textStyle = TextStyle(fontFamily = Jakarta, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium, color = Ink))
            if (upi.isNotBlank()) Column(Modifier.fillMaxWidth().background(Color(0xFFF4F0FF), RoundedCornerShape(14.dp)).padding(12.dp)) { Text("PAYMENT LINK INCLUDED", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold); Text(upi, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold) } else TextButton(onClick = settings) { Text("Add UPI ID for a payment link") }
        }
        Text("You’ll review and send it yourself in WhatsApp.", color = Muted, fontSize = 12.sp)
        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
        PrimaryButton("Continue to WhatsApp", {
            if (text.isBlank()) error = "Write a message first." else try {
                val shareText = if (upi.isBlank()) text else "$text\n\nPay via UPI: ${upiLink(upi, d)}\nUPI ID: $upi"
                val url = "https://wa.me/${d.phone}?text=${Uri.encode(shareText)}"
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                onOpened()
            } catch (_: Exception) { error = "Couldn't open WhatsApp. Check that WhatsApp or a browser is installed." }
        })
    }
    confirmTone?.let { next -> AlertDialog(onDismissRequest = { confirmTone = null }, title = { Text("Change the tone?") }, text = { Text("This replaces the edits you made to the message.") }, confirmButton = { TextButton(onClick = { chooseTone(next); confirmTone = null }) { Text("Change tone") } }, dismissButton = { TextButton(onClick = { confirmTone = null }) { Text("Keep message") } }) }
}
