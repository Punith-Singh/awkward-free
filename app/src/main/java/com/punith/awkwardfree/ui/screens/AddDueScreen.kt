package com.punith.awkwardfree.ui.screens

import android.app.DatePickerDialog
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

@Composable internal fun AddScreen(existing: Due?, back: () -> Unit, save: (Due) -> Unit) {
    var name by remember(existing?.id) { mutableStateOf(existing?.name ?: "") }; var amount by remember(existing?.id) { mutableStateOf(existing?.amount ?: "") }; var reason by remember(existing?.id) { mutableStateOf(existing?.reason ?: "") }; var date by remember(existing?.id) { mutableStateOf(existing?.date ?: LocalDate.now().toString()) }; var phone by remember(existing?.id) { mutableStateOf(existing?.phone ?: "") }; var error by remember { mutableStateOf("") }
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        BackHeader(if (existing == null) "New tab" else "Edit tab", back)
        Heading("Keep it simple", if (existing == null) "Add a little tab." else "Edit your tab.", "Just the details you need to remember.")
        Column(Modifier.fillMaxWidth().background(Deep, RoundedCornerShape(22.dp)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("HOW MUCH?", color = Color(0xFFD2C9FF), fontWeight = FontWeight.Bold, fontSize = 11.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("₹ ", color = Color(0xFFB8A9FF), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                BasicTextField(value = amount, onValueChange = { amount = it }, singleLine = true, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), textStyle = TextStyle(fontFamily = Jakarta, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = Color.White), decorationBox = { inner -> Box { if (amount.isEmpty()) Text("0", color = Color(0xFFB8A9FF), fontSize = 36.sp, fontWeight = FontWeight.ExtraBold); inner() } })
            }
            Text("Every rupee deserves its way home.", color = Color(0xFFDCD5FF), fontSize = 12.sp)
        }
        Input(name, { name = it }, "Who owes you?", "Ananya")
        Input(reason, { reason = it }, "What was it for?", "Dinner at Toit")
        val parsed = try { LocalDate.parse(date) } catch (_: Exception) { LocalDate.now() }
        OutlinedButton(onClick = { DatePickerDialog(context, { _, year, month, day -> date = LocalDate.of(year, month + 1, day).toString() }, parsed.year, parsed.monthValue - 1, parsed.dayOfMonth).show() }, modifier = Modifier.fillMaxWidth().height(62.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White), border = null) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Due  ${dateLabel(date)}", color = Ink); Text("Calendar", color = Violet) } }
        Input(phone, { phone = it }, "WhatsApp number (optional)", "919876543210", KeyboardType.Phone)
        Text("Include the country code, digits only. Or choose a contact in WhatsApp.", color = Muted, fontSize = 12.sp)
        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
        PrimaryButton(if (existing == null) "Save this tab" else "Save changes", {
            val n = amount.toBigDecimalOrNull()
            error = when { name.isBlank() || reason.isBlank() -> "Add a name and reason."; n == null || n <= BigDecimal.ZERO || n.scale() > 2 || n > BigDecimal("10000000") -> "Enter a valid amount greater than zero with up to 2 decimal places."; phone.isNotBlank() && !phone.matches(Regex("[1-9][0-9]{7,14}")) -> "Enter the phone number with country code, using digits only."; else -> "" }
            if (error.isEmpty()) save((existing ?: Due(name = "", amount = "", reason = "", date = date)).copy(name = name.trim(), amount = n!!.setScale(2).toPlainString(), reason = reason.trim(), date = date, phone = phone))
        })
    }
}
