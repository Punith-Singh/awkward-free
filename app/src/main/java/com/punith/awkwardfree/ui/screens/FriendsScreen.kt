package com.punith.awkwardfree.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.punith.awkwardfree.util.createFriendStatement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.punith.awkwardfree.model.*
import com.punith.awkwardfree.ui.components.*
import com.punith.awkwardfree.ui.theme.*
import com.punith.awkwardfree.util.money
import com.punith.awkwardfree.util.upiLink
import java.math.BigDecimal
import java.util.Locale

@Composable internal fun FriendsScreen(
    dues: List<Due>, upi: String, details: (Due) -> Unit, nudge: (Due) -> Unit,
    openSplit: (String) -> Unit, onOffset: (Due) -> Unit, onGroupOpened: (Set<String>) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingReminder by remember { mutableStateOf<List<Due>?>(null) }
    var message by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var pendingOffset by remember { mutableStateOf<Due?>(null) }
    val groups = dues.filterNot { it.settled }.groupBy { due ->
        normalizePhone(due.phone)?.let { "${due.name.trim().lowercase(Locale.ROOT)}:$it" } ?: "entry:${due.id}"
    }.values.sortedBy { it.first().name.lowercase(Locale.ROOT) }
    val splits = dues.filter { it.splitGroupId.isNotBlank() }.groupBy { it.splitGroupId }.values.filter { group -> group.any { !it.settled } }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 20.dp, bottom = 112.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("YOUR PEOPLE", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                Text("Friends", color = Ink, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                Text("All your tabs together, person by person.", color = Muted, style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (groups.isEmpty()) item { EmptyCard("Your people, all in one place", "Add a tab and your friends’ balances will appear here.") }
        items(groups, key = { it.first().id }) { group ->
            val first = group.first()
            val incoming = group.filterNot { it.owedByMe }
            val receivable = incoming.fold(BigDecimal.ZERO) { sum, due -> sum + due.remaining() }
            val payable = group.filter { it.owedByMe }.fold(BigDecimal.ZERO) { sum, due -> sum + due.remaining() }
            val net = receivable - payable
            Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(24.dp)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Avatar(first.name)
                    Column(Modifier.weight(1f)) {
                        Text(first.name, color = Ink, fontWeight = FontWeight.Bold)
                        Text("${group.size} open ${if (group.size == 1) "tab" else "tabs"}", color = Muted, style = MaterialTheme.typography.bodySmall)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(if (net.signum() > 0) "+${money(net.toPlainString())}" else if (net.signum() < 0) "−${money(net.abs().toPlainString())}" else money("0"), color = if (net.signum() < 0) MaterialTheme.colorScheme.error else Violet, fontWeight = FontWeight.ExtraBold)
                        Text(if (normalizePhone(first.phone) == null) "this tab" else "net", color = Muted, style = MaterialTheme.typography.labelSmall)
                    }
                }
                HorizontalDivider(color = Line)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    if (incoming.isNotEmpty()) TextButton(onClick = {
                        if (incoming.size == 1) nudge(incoming.first())
                        else {
                            val lines = incoming.joinToString("\n") { "• ${it.reason}: ${money(it.remaining().toPlainString())}" }
                            val payment = if (upi.isNotBlank()) {
                                val aggregate = incoming.first().copy(amount = receivable.toPlainString(), paidAmount = "0.00", reason = "Shared tabs")
                                "\n\nPay via UPI: ${upiLink(upi, aggregate)}\nUPI ID: $upi"
                            } else ""
                            message = "Hi ${first.name}, just a friendly reminder about these tabs:\n$lines\nTotal: ${money(receivable.toPlainString())}$payment"
                            error = ""
                            pendingReminder = incoming
                        }
                    }) { Text(if (incoming.size > 1) "Remind all" else "Remind") }
                    if (receivable.signum() > 0 && payable.signum() > 0 && normalizePhone(first.phone) != null) {
                        TextButton(onClick = { pendingOffset = first }) { Text("Offset") }
                    }
                    TextButton(onClick = { details(first) }) { Text("Details") }
                }
                TextButton(onClick = {
                    val snapshot = group.toList()
                    scope.launch {
                        try {
                            val uri = withContext(Dispatchers.IO) { createFriendStatement(context, snapshot) }
                            val send = Intent(Intent.ACTION_SEND).setType("application/pdf")
                                .putExtra(Intent.EXTRA_STREAM, uri)
                                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            context.startActivity(Intent.createChooser(send, "Share statement"))
                        } catch (_: Exception) {
                            Toast.makeText(context, "Couldn’t create or share the statement.", Toast.LENGTH_LONG).show()
                        }
                    }
                }) { Text("Share PDF statement") }
            }
        }
        if (splits.isNotEmpty()) item { Text("Split expenses", color = Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp) }
        items(splits, key = { "split:" + it.first().splitGroupId }) { group ->
            val total = group.fold(BigDecimal.ZERO) { sum, due -> sum + (due.amount.toBigDecimalOrNull() ?: BigDecimal.ZERO) }
            OutlinedButton(onClick = { openSplit(group.first().splitGroupId) }, modifier = Modifier.fillMaxWidth()) {
                Text("${group.first().reason} · ${money(total.toPlainString())} · ${group.count { it.settled }} of ${group.size} settled")
            }
        }
    }
    pendingOffset?.let { due ->
        friendBalance(due, dues)?.let { balance ->
            AlertDialog(onDismissRequest = { pendingOffset = null }, title = { Text("Offset tabs with ${due.name}?") },
                text = { Text("Record ${money(balance.owedToMe.min(balance.iOwe).toPlainString())} against tabs in each direction, oldest first. This does not transfer money. Confirm only if you both agreed.") },
                confirmButton = { TextButton(onClick = { onOffset(due); pendingOffset = null }) { Text("Record offset") } },
                dismissButton = { TextButton(onClick = { pendingOffset = null }) { Text("Cancel") } })
        }
    }
    pendingReminder?.let { group ->
        AlertDialog(onDismissRequest = { pendingReminder = null }, title = { Text("Review reminder") },
            text = { Column {
                Text("Review and edit before opening WhatsApp.")
                OutlinedTextField(value = message, onValueChange = { message = it }, modifier = Modifier.fillMaxWidth(), minLines = 4, label = { Text("Message") })
                if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
            } },
            confirmButton = { TextButton(onClick = {
                val phone = normalizePhone(group.first().phone)
                if (message.isBlank()) error = "Write a message first."
                else if (phone == null) error = "Add a valid phone number first."
                else try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$phone?text=${Uri.encode(message)}")))
                    onGroupOpened(group.map { it.id }.toSet())
                    pendingReminder = null
                } catch (_: Exception) { error = "Couldn’t open WhatsApp." }
            }) { Text("Open WhatsApp") } },
            dismissButton = { TextButton(onClick = { pendingReminder = null }) { Text("Cancel") } })
    }
}
