package com.punith.awkwardfree.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.punith.awkwardfree.model.Due
import com.punith.awkwardfree.model.splitEqually
import com.punith.awkwardfree.ui.components.BackHeader
import com.punith.awkwardfree.ui.components.Heading
import com.punith.awkwardfree.ui.components.PrimaryButton
import com.punith.awkwardfree.util.dateLabel
import com.punith.awkwardfree.util.money
import java.math.BigDecimal
import java.time.LocalDate

@Composable internal fun SplitDueScreen(back: () -> Unit, save: (List<Due>) -> Unit) {
    val context = LocalContext.current
    var names by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf(LocalDate.now()) }
    var error by remember { mutableStateOf("") }
    var preview by remember { mutableStateOf<List<Due>?>(null) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        BackHeader("Split expense", back)
        Heading("SPLIT EQUALLY", "Split with friends.", "Enter the amount your friends owe in total. Your own share should already be excluded.")
        OutlinedTextField(value = amount, onValueChange = { amount = it; error = "" }, label = { Text("Total to collect (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = names, onValueChange = { names = it; error = "" }, label = { Text("Friends, separated by commas") }, placeholder = { Text("Rahul, Ananya") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = reason, onValueChange = { reason = it; error = "" }, label = { Text("What was it for?") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedButton(onClick = {
            DatePickerDialog(context, { _, year, month, day -> dueDate = LocalDate.of(year, month + 1, day) }, dueDate.year, dueDate.monthValue - 1, dueDate.dayOfMonth).show()
        }, modifier = Modifier.fillMaxWidth()) { Text("Due ${dateLabel(dueDate.toString())}") }
        if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error)
        PrimaryButton("Review shares", {
            val people = names.split(',').map { it.trim() }.filter { it.isNotEmpty() }
            val total = amount.toBigDecimalOrNull()
            error = when {
                people.size !in 2..50 -> "Enter between 2 and 50 friends."
                people.any { it.length > 500 } -> "A name is too long."
                people.map { it.lowercase() }.toSet().size != people.size -> "Please use distinct names for each friend."
                reason.isBlank() || reason.length > 1000 -> "Enter a reason."
                total == null || total <= BigDecimal.ZERO || total.scale() > 2 || total > BigDecimal("10000000") -> "Enter a valid total with up to two decimal places."
                else -> ""
            }
            if (error.isEmpty() && total != null) {
                val shares = runCatching { splitEqually(total, people.size) }.getOrNull()
                if (shares == null) error = "The amount must cover at least one paisa per friend."
                else preview = people.zip(shares).map { (person, share) -> Due(name = person, amount = share.toPlainString(), reason = reason.trim(), date = dueDate.toString()) }
            }
        })
    }
    preview?.let { entries ->
        AlertDialog(
            onDismissRequest = { preview = null },
            title = { Text("Review the split") },
            text = { Column(Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
                Text("A separate tab will be created for each friend.")
                entries.forEach { Text("${it.name}: ${money(it.amount)}") }
            } },
            confirmButton = { TextButton(onClick = { preview = null; save(entries) }) { Text("Create tabs") } },
            dismissButton = { TextButton(onClick = { preview = null }) { Text("Back") } }
        )
    }
}
