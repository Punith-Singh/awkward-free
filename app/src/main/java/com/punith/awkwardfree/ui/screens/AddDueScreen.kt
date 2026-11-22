package com.punith.awkwardfree.ui.screens

import android.app.DatePickerDialog
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.ContactsContract
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.punith.awkwardfree.model.*
import com.punith.awkwardfree.ui.components.*
import com.punith.awkwardfree.ui.theme.*
import com.punith.awkwardfree.util.*
import java.math.BigDecimal
import java.time.LocalDate

@Composable internal fun AddScreen(existing: Due?, back: () -> Unit, duplicating: Boolean = false, save: (Due) -> Unit) {
    var name by remember(existing?.id) { mutableStateOf(existing?.name ?: "") }; var amount by remember(existing?.id) { mutableStateOf(existing?.amount ?: "") }; var reason by remember(existing?.id) { mutableStateOf(existing?.reason ?: "") }; var date by remember(existing?.id) { mutableStateOf(existing?.date ?: LocalDate.now().toString()) }; var phone by remember(existing?.id) { mutableStateOf(existing?.phone ?: "") }; var error by remember { mutableStateOf("") }
    var owedByMe by remember(existing?.id) { mutableStateOf(existing?.owedByMe ?: false) }
    var repeatMonthly by remember(existing?.id) { mutableStateOf(existing?.repeatMonthly ?: false) }
    val context = LocalContext.current
    var voiceMessage by remember { mutableStateOf("") }
    fun fillFromSpeech(transcript: String) {
        val details = parseVoiceDue(transcript)
        details.name?.let { name = it }
        details.amount?.let { amount = it }
        details.reason?.let { reason = it }
        voiceMessage = "Heard: $transcript\nReview all fields and choose a due date before saving."
    }
    var contactMessage by remember { mutableStateOf("") }
    val contactLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.data
        if (result.resultCode == Activity.RESULT_OK && uri != null) {
            try {
                val columns = arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER)
                context.contentResolver.query(uri, columns, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val selectedName = cursor.getString(0).orEmpty().trim()
                        val rawNumber = cursor.getString(1).orEmpty()
                        var digits = rawNumber.filter { it.isDigit() }
                        if (digits.startsWith("00")) digits = digits.drop(2)
                        if (!rawNumber.trim().startsWith("+") && digits.length == 10 && digits.firstOrNull() in listOf('6', '7', '8', '9')) {
                            digits = "91" + digits
                        }
                        if (selectedName.isNotEmpty()) name = selectedName
                        if (digits.matches(Regex("[1-9][0-9]{7,14}"))) {
                            phone = digits
                            contactMessage = "Contact added. Check the country code before saving."
                        } else {
                            phone = ""
                            contactMessage = "Couldn't use this number. Enter a WhatsApp number with its country code."
                        }
                    }
                } ?: run { contactMessage = "Couldn't read this contact. Enter the details manually." }
            } catch (_: Exception) {
                contactMessage = "Couldn't read this contact. Enter the details manually."
            }
        }
    }
    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val transcript = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (transcript.isNullOrBlank()) {
                voiceMessage = "No speech was recognized. Please try again."
            } else {
                fillFromSpeech(transcript)
            }
        } else {
            voiceMessage = "Voice entry cancelled. You can still enter the details below."
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        BackHeader(if (duplicating) "Duplicate tab" else if (existing == null) "New tab" else "Edit tab", back)
        Heading("Keep it simple", if (duplicating) "Duplicate your tab." else if (existing == null) "Add a little tab." else "Edit your tab.", "Just the details you need to remember.")
        if (existing == null) {
            VoiceCaptureButton(onText = ::fillFromSpeech, fallback = {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Say: Rahul borrowed 500 rupees for lunch")
                }
                try { voiceLauncher.launch(intent) } catch (_: ActivityNotFoundException) {
                    voiceMessage = "Speech recognition isn't available on this device. Enter the details below instead."
                }
            })
            Text("Try: Rahul borrowed 500 rupees for lunch", color = Muted, fontSize = 12.sp)
            Text("Voice recognition may use your device's speech service and send audio for processing.", color = Muted, fontSize = 12.sp)
            if (voiceMessage.isNotBlank()) Text(voiceMessage, color = Ink, fontSize = 13.sp)
        }
        Column(Modifier.fillMaxWidth().background(Deep, RoundedCornerShape(22.dp)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("HOW MUCH?", color = Color(0xFFD2C9FF), fontWeight = FontWeight.Bold, fontSize = 11.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("₹ ", color = Color(0xFFB8A9FF), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                BasicTextField(value = amount, onValueChange = { amount = it }, singleLine = true, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), textStyle = TextStyle(fontFamily = Jakarta, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = Color.White), decorationBox = { inner -> Box { if (amount.isEmpty()) Text("0", color = Color(0xFFB8A9FF), fontSize = 36.sp, fontWeight = FontWeight.ExtraBold); inner() } })
            }
            Text("Every rupee deserves its way home.", color = Color(0xFFDCD5FF), fontSize = 12.sp)
        }
        Text("DIRECTION", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !owedByMe, onClick = { owedByMe = false }, label = { Text("Owed to me") })
            FilterChip(selected = owedByMe, onClick = { owedByMe = true }, label = { Text("I owe") })
        }
        Input(name, { name = it }, if (owedByMe) "Who do you owe?" else "Who owes you?", "Ananya")
        OutlinedButton(onClick = {
            try {
                contactLauncher.launch(Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI))
            } catch (_: ActivityNotFoundException) {
                contactMessage = "Contact picker is unavailable. Enter the details manually."
            }
        }, modifier = Modifier.fillMaxWidth()) { Text("Choose from contacts") }
        if (contactMessage.isNotBlank()) Text(contactMessage, color = Muted, fontSize = 12.sp)
        Input(reason, { reason = it }, "What was it for?", "Dinner at Toit")
        val parsed = try { LocalDate.parse(date) } catch (_: Exception) { LocalDate.now() }
        OutlinedButton(onClick = { DatePickerDialog(context, { _, year, month, day -> date = LocalDate.of(year, month + 1, day).toString() }, parsed.year, parsed.monthValue - 1, parsed.dayOfMonth).show() }, modifier = Modifier.fillMaxWidth().height(62.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White), border = null) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Due  ${dateLabel(date)}", color = Ink); Text("Calendar", color = Violet) } }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text("Repeat monthly", fontWeight = FontWeight.Bold, color = Ink)
                Text("Creates a new tab each month. Missed months appear when the app opens.", color = Muted, fontSize = 12.sp)
            }
            Switch(checked = repeatMonthly, onCheckedChange = { repeatMonthly = it })
        }
        Input(phone, { phone = it }, "WhatsApp number (optional)", "919876543210", KeyboardType.Phone)
        Text("Include the country code, digits only. Or choose a contact in WhatsApp.", color = Muted, fontSize = 12.sp)
        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
        PrimaryButton(if (existing == null || duplicating) "Save this tab" else "Save changes", {
            val n = amount.toBigDecimalOrNull()
            error = when { name.isBlank() || reason.isBlank() -> "Add a name and reason."; n == null || n <= BigDecimal.ZERO || n.scale() > 2 || n > BigDecimal("10000000") -> "Enter a valid amount greater than zero with up to 2 decimal places."; existing != null && n != null && n < (existing.paidAmount.toBigDecimalOrNull() ?: BigDecimal.ZERO) -> "The total cannot be less than payments already recorded."; existing != null && n != null && n.compareTo(existing.paidAmount.toBigDecimalOrNull() ?: BigDecimal.ZERO) == 0 && !existing.settled -> "The total must be greater than payments already recorded."; phone.isNotBlank() && !phone.matches(Regex("[1-9][0-9]{7,14}")) -> "Enter the phone number with country code, using digits only."; else -> "" }
            if (error.isEmpty()) save((existing ?: Due(name = "", amount = "", reason = "", date = date)).copy(name = name.trim(), amount = n!!.setScale(2).toPlainString(), reason = reason.trim(), date = date, phone = phone, owedByMe = owedByMe, repeatMonthly = repeatMonthly, recurrenceThrough = if (repeatMonthly && existing != null && !existing.repeatMonthly && existing.recurrenceThrough.isNotBlank() && existing.date == date) maxOf(LocalDate.now(), LocalDate.parse(date)).toString() else if (existing != null && existing.date == date) existing.recurrenceThrough.ifBlank { if (repeatMonthly) date else "" } else if (repeatMonthly) date else ""))
        })
    }
}
