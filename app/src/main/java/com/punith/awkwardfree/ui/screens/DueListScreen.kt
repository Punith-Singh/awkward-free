package com.punith.awkwardfree.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.punith.awkwardfree.model.Due
import com.punith.awkwardfree.model.friendBalance
import com.punith.awkwardfree.ui.components.*
import com.punith.awkwardfree.ui.theme.*
import com.punith.awkwardfree.util.money
import com.punith.awkwardfree.util.upiLink
import java.math.BigDecimal
import java.time.LocalDate

private fun overdue(due: Due, today: LocalDate): Boolean =
    !due.settled && runCatching { LocalDate.parse(due.date).isBefore(today) }.getOrDefault(false)

@Composable internal fun ListScreen(
    title: String, subtitle: String, dues: List<Due>, settled: Boolean,
    back: () -> Unit, details: (Due) -> Unit, nudge: (Due) -> Unit,
    upi: String = "", onGroupOpened: (Set<String>) -> Unit = {}
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var pendingGroup by remember { mutableStateOf<List<Due>?>(null) }
    var groupMessage by remember { mutableStateOf("") }
    var groupError by remember { mutableStateOf("") }
    var onlyOverdue by remember { mutableStateOf(false) }
    var grouped by remember { mutableStateOf(false) }
    var sort by remember { mutableStateOf("Due date") }
    val today = LocalDate.now()
    val visible = dues.asSequence()
        .filter { query.isBlank() || it.name.contains(query.trim(), ignoreCase = true) ||
            it.reason.contains(query.trim(), ignoreCase = true) }
        .filter { !onlyOverdue || overdue(it, today) }
        .let { sequence ->
            when (sort) {
                "Amount" -> sequence.sortedByDescending { if (settled) it.amount.toBigDecimalOrNull() ?: BigDecimal.ZERO else it.remaining() }
                "Name" -> sequence.sortedBy { it.name.lowercase() }
                else -> sequence.sortedBy { it.date }
            }
        }.toList()

    // Missing phone numbers are kept separate: a shared name is not proof of identity.
    val groups = visible.groupBy {
        if (it.phone.isBlank()) "entry:${it.id}"
        else "person:${it.name.trim().lowercase()}:${it.phone}:${it.owedByMe}"
    }.values.toList()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(22.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        item { BackHeader(if (settled) "Settled" else "Your tabs", back) }
        item { Heading(if (settled) "GOOD FRIENDS. CLEAR TABS." else "KEEP TRACK", title, subtitle) }
        item {
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                label = { Text("Search name or reason") }
            )
        }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = grouped, onClick = { grouped = !grouped }, label = { Text("Group by friend") })
                if (!settled) FilterChip(selected = onlyOverdue, onClick = { onlyOverdue = !onlyOverdue }, label = { Text("Overdue") })
            }
        }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Due date", "Amount", "Name").forEach { option ->
                    FilterChip(selected = sort == option, onClick = { sort = option }, label = { Text(option) })
                }
            }
        }
        item { Pill("${visible.size} ${if (settled) "SETTLED" else "OPEN"}", Lavender, Violet) }
        if (visible.isEmpty()) {
            item { EmptyCard(if (dues.isEmpty()) "No tabs yet" else "No matching tabs", if (dues.isEmpty()) "Your tabs will appear here." else "Try a different search or filter.") }
        } else if (grouped) {
            item { Text("Tabs are grouped when name, phone number and direction match. Tabs without a phone number stay separate.", color = Muted, style = MaterialTheme.typography.bodySmall) }
            items(groups, key = { group -> group.first().id }) { group ->
                Column(Modifier.fillMaxWidth().background(Color(0xFFEEEAFD), RoundedCornerShape(20.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val total = group.fold(BigDecimal.ZERO) { sum, due -> sum + if (settled) (due.amount.toBigDecimalOrNull() ?: BigDecimal.ZERO) else due.remaining() }
                    Text(group.first().name + if (group.first().owedByMe) " · I owe" else " · Owed to me", color = Ink, fontWeight = FontWeight.Bold)
                    Text("${group.size} ${if (group.size == 1) "tab" else "tabs"} · ${money(total.toPlainString())}", color = Violet)
                    if (!settled) friendBalance(group.first(), dues)?.let { balance ->
                        Text("All open tabs: owed to you ${money(balance.owedToMe.toPlainString())} · you owe ${money(balance.iOwe.toPlainString())}", color = Ink, style = MaterialTheme.typography.bodySmall)
                        val netLabel = when (balance.net.signum()) {
                            1 -> "Net owed to you: ${money(balance.net.toPlainString())}"
                            -1 -> "Net you owe: ${money(balance.net.abs().toPlainString())}"
                            else -> "Net: all square"
                        }
                        Text(netLabel, color = Violet, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    }
                    if (!settled && group.size > 1) {
                        OutlinedButton(onClick = {
                            val lines = group.joinToString("\n") { "• ${it.reason}: ${money(it.remaining().toPlainString())}" }
                            val direction = if (group.first().owedByMe) "I owe you" else "You owe me"
                            val statement = "Hi ${group.first().name}, here's a summary of the tabs:\n$lines\nTotal $direction: ${money(total.toPlainString())}"
                            val intent = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, statement)
                            context.startActivity(Intent.createChooser(intent, "Share statement"))
                        }) { Text("Share visible tabs") }
                    }
                    if (!settled && !group.first().owedByMe && group.size > 1) {
                        OutlinedButton(onClick = {
                            val lines = group.joinToString("\n") { "• ${it.reason}: ${money(it.remaining().toPlainString())}" }
                            val totalDue = group.fold(BigDecimal.ZERO) { sum, due -> sum + due.remaining() }
                            val payment = if (upi.isNotBlank()) {
                                val aggregate = group.first().copy(amount = totalDue.toPlainString(), paidAmount = "0.00", reason = "Shared tabs")
                                "\n\nPay via UPI: ${upiLink(upi, aggregate)}\nUPI ID: $upi"
                            } else ""
                            groupMessage = "Hi ${group.first().name}, just a friendly reminder about these tabs:\n$lines\nTotal: ${money(totalDue.toPlainString())}$payment"
                            groupError = ""
                            pendingGroup = group.toList()
                        }) { Text("Send one WhatsApp reminder") }
                    }
                    group.forEach { due -> DueCard(due, { details(due) }, { nudge(due) }) }
                }
            }
        } else {
            items(visible, key = { it.id }) { due -> DueCard(due, { details(due) }, { nudge(due) }) }
        }
    }
    pendingGroup?.let { group ->
        AlertDialog(
            onDismissRequest = { pendingGroup = null },
            title = { Text("Review reminder") },
            text = {
                Column {
                    Text("Review and edit this message before opening WhatsApp.")
                    OutlinedTextField(value = groupMessage, onValueChange = { groupMessage = it }, modifier = Modifier.fillMaxWidth(), minLines = 4, label = { Text("Message") })
                    if (groupError.isNotBlank()) Text(groupError, color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val phone = group.first().phone
                    if (groupMessage.isBlank()) groupError = "Write a message first."
                    else if (!phone.matches(Regex("[1-9][0-9]{7,14}"))) groupError = "A valid number with country code is required."
                    else try {
                        val uri = Uri.parse("https://wa.me/$phone?text=${Uri.encode(groupMessage)}")
                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        onGroupOpened(group.map { it.id }.toSet())
                        pendingGroup = null
                    } catch (_: Exception) { groupError = "Couldn't open WhatsApp. Try again." }
                }) { Text("Open WhatsApp") }
            },
            dismissButton = { TextButton(onClick = { pendingGroup = null }) { Text("Cancel") } }
        )
    }

}
