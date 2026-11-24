package com.punith.awkwardfree.ui.screens

import android.app.DatePickerDialog
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.punith.awkwardfree.model.splitShares
import java.util.UUID
import com.punith.awkwardfree.model.normalizePhone
import java.util.Locale
import com.punith.awkwardfree.ui.components.BackHeader
import com.punith.awkwardfree.ui.components.Heading
import com.punith.awkwardfree.ui.components.PrimaryButton
import com.punith.awkwardfree.util.dateLabel
import com.punith.awkwardfree.util.money
import java.math.BigDecimal
import java.time.LocalDate

private data class SplitPerson(val name: String = "", val phone: String = "", val share: String = "")

@Composable internal fun SplitDueScreen(existingDues: List<Due>, back: () -> Unit, save: (List<Due>) -> Unit) {
    val context = LocalContext.current
    val people = remember { mutableStateListOf(SplitPerson(), SplitPerson()) }
    var amount by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf("Equal") }
    var includeMe by remember { mutableStateOf(false) }
    var myShare by remember { mutableStateOf("") }
    var paidByMe by remember { mutableStateOf(true) }
    var payerIndex by remember { mutableIntStateOf(0) }
    var contactIndex by remember { mutableIntStateOf(-1) }
    var reason by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf(LocalDate.now()) }
    var error by remember { mutableStateOf("") }
    var preview by remember { mutableStateOf<List<Due>?>(null) }
    val contactLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.data
        val index = contactIndex
        if (result.resultCode == Activity.RESULT_OK && uri != null && index in people.indices) {
            try {
                val columns = arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER)
                context.contentResolver.query(uri, columns, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val raw = cursor.getString(1).orEmpty()
                        people[index] = people[index].copy(name = cursor.getString(0).orEmpty().trim(), phone = normalizePhone(raw) ?: raw)
                    }
                }
            } catch (_: Exception) { error = "Could not read contact. Enter their details manually." }
        }
    }
    val savedPeople = existingDues.filter { it.name.isNotBlank() && normalizePhone(it.phone) != null }
        .distinctBy { it.name.trim().lowercase(Locale.ROOT) to normalizePhone(it.phone) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        BackHeader("Split expense", back)
        Heading("SPLIT EXPENSE", "Split with friends.", "Choose who paid and how to divide the bill.")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = paidByMe, onClick = { paidByMe = true }, label = { Text("I paid") })
            FilterChip(selected = !paidByMe, onClick = { paidByMe = false; includeMe = true }, label = { Text("A friend paid") })
        }
        if (!paidByMe) Text("Only your share owed to the payer is recorded; the other friends’ debts are not tracked.", style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(value = amount, onValueChange = { amount = it; error = "" }, label = { Text("Bill total (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Equal", "Amount", "Percent").forEach { option ->
                FilterChip(selected = mode == option, onClick = { mode = option; error = "" }, label = { Text(option) })
            }
        }
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Checkbox(checked = includeMe, onCheckedChange = { includeMe = it; error = "" }, enabled = paidByMe)
            Text("Include my share")
        }
        if (includeMe && mode != "Equal") OutlinedTextField(value = myShare, onValueChange = { myShare = it; error = "" }, label = { Text(if (mode == "Percent") "My share (%)" else "My share (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
        people.forEachIndexed { index, person ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Friend ${index + 1}", style = MaterialTheme.typography.titleSmall)
                OutlinedTextField(value = person.name, onValueChange = { people[index] = person.copy(name = it); error = "" }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = person.phone, onValueChange = { people[index] = person.copy(phone = it); error = "" }, label = { Text("WhatsApp number (optional)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedButton(onClick = {
                    contactIndex = index
                    try { contactLauncher.launch(Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)) }
                    catch (_: ActivityNotFoundException) { error = "Contact picker unavailable. Enter the details manually." }
                }) { Text("Choose contact") }
                if (person.name.isNotBlank()) savedPeople.filter { it.name.contains(person.name, ignoreCase = true) }.take(3).forEach { saved ->
                    TextButton(onClick = { people[index] = person.copy(name = saved.name, phone = saved.phone) }) { Text("Use ${saved.name} · +${normalizePhone(saved.phone)}") }
                }
                if (!paidByMe) Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    RadioButton(selected = payerIndex == index, onClick = { payerIndex = index })
                    Text("This friend paid")
                }
                if (mode != "Equal") OutlinedTextField(value = person.share, onValueChange = { people[index] = person.copy(share = it); error = "" }, label = { Text(if (mode == "Percent") "Share (%)" else "Share (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
                if (people.size > if (includeMe) 1 else 2) TextButton(onClick = { people.removeAt(index); payerIndex = 0 }) { Text("Remove friend") }
            }
        }
        Text("Indian mobile numbers default to +91. Add a country code for other numbers.", style = MaterialTheme.typography.bodySmall)
        if (people.size < 50) OutlinedButton(onClick = { people.add(SplitPerson()) }) { Text("Add another friend") }
        OutlinedTextField(value = reason, onValueChange = { reason = it; error = "" }, label = { Text("What was it for?") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedButton(onClick = {
            DatePickerDialog(context, { _, year, month, day -> dueDate = LocalDate.of(year, month + 1, day) }, dueDate.year, dueDate.monthValue - 1, dueDate.dayOfMonth).show()
        }, modifier = Modifier.fillMaxWidth()) { Text("Due ${dateLabel(dueDate.toString())}") }
        if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error)
        PrimaryButton("Review shares", {
            val participants = people.map { it.copy(name = it.name.trim()) }
            val total = amount.toBigDecimalOrNull()
            error = when {
                participants.size !in (if (includeMe) 1 else 2)..50 -> "Add at least one friend and include your share, or add two friends."
                participants.any { it.name.isBlank() || it.name.length > 500 } -> "Enter a valid name for each friend."
                participants.map { it.name.lowercase(Locale.ROOT) to (normalizePhone(it.phone) ?: "") }.toSet().size != participants.size -> "Each friend must have a distinct name and number."
                participants.any { it.phone.isNotBlank() && normalizePhone(it.phone) == null } -> "Enter a valid phone number for each friend, or leave it blank."
                reason.isBlank() || reason.length > 1000 -> "Enter a reason."
                total == null || total <= BigDecimal.ZERO || total.scale() > 2 || total > BigDecimal("10000000") -> "Enter a valid total with up to two decimal places."
                else -> ""
            }
            if (error.isEmpty() && total != null) {
                val inputs = (if (includeMe) listOf(myShare) else emptyList()) + participants.map { it.share }
                val result = runCatching { splitShares(total, mode, inputs) }
                val shares = result.getOrNull()
                if (shares == null) error = result.exceptionOrNull()?.message ?: "Check the shares and total."
                else {
                    val groupId = UUID.randomUUID().toString()
                    val friendShares = if (includeMe) shares.drop(1) else shares
                    preview = if (paidByMe) participants.zip(friendShares).map { (person, share) -> Due(name = person.name, phone = normalizePhone(person.phone) ?: "", amount = share.toPlainString(), reason = reason.trim(), date = dueDate.toString(), splitGroupId = groupId) }
                    else {
                        val payer = participants[payerIndex.coerceIn(participants.indices)]
                        listOf(Due(name = payer.name, phone = normalizePhone(payer.phone) ?: "", amount = shares.first().toPlainString(), reason = reason.trim(), date = dueDate.toString(), splitGroupId = groupId, owedByMe = true))
                    }
                }
            }
        })
    }
    preview?.let { entries ->
        AlertDialog(
            onDismissRequest = { preview = null },
            title = { Text("Review the split") },
            text = { Column(Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
                Text(if (paidByMe) "A separate tab will be created for each friend." else "One tab will record your share owed to the payer.")
                entries.forEach { Text("${it.name}: ${money(it.amount)}${if (it.phone.isBlank()) " · no number" else " · +${it.phone}"}") }
            } },
            confirmButton = { TextButton(onClick = { preview = null; save(entries) }) { Text("Create tabs") } },
            dismissButton = { TextButton(onClick = { preview = null }) { Text("Back") } }
        )
    }
}
