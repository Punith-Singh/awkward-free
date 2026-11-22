package com.punith.awkwardfree.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.punith.awkwardfree.model.Due
import com.punith.awkwardfree.ui.components.*
import com.punith.awkwardfree.ui.theme.*
import com.punith.awkwardfree.util.money
import java.math.BigDecimal
import java.time.LocalDate

private fun overdue(due: Due, today: LocalDate): Boolean =
    !due.settled && runCatching { LocalDate.parse(due.date).isBefore(today) }.getOrDefault(false)

@Composable internal fun ListScreen(
    title: String, subtitle: String, dues: List<Due>, settled: Boolean,
    back: () -> Unit, details: (Due) -> Unit, nudge: (Due) -> Unit
) {
    var query by remember { mutableStateOf("") }
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
                "Amount" -> sequence.sortedByDescending { it.remaining() }
                "Name" -> sequence.sortedBy { it.name.lowercase() }
                else -> sequence.sortedBy { it.date }
            }
        }.toList()

    // Missing phone numbers are kept separate: a shared name is not proof of identity.
    val groups = visible.groupBy {
        if (it.phone.isBlank()) "entry:${it.id}"
        else "person:${it.name.trim().lowercase()}:${it.phone}"
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
            item { Text("Tabs are grouped when both the name and phone number match. Tabs without a phone number stay separate.", color = Muted, style = MaterialTheme.typography.bodySmall) }
            items(groups, key = { group -> group.first().id }) { group ->
                Column(Modifier.fillMaxWidth().background(Color(0xFFEEEAFD), RoundedCornerShape(20.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val total = group.fold(BigDecimal.ZERO) { sum, due -> sum + if (settled) (due.amount.toBigDecimalOrNull() ?: BigDecimal.ZERO) else due.remaining() }
                    Text(group.first().name, color = Ink, fontWeight = FontWeight.Bold)
                    Text("${group.size} ${if (group.size == 1) "tab" else "tabs"} · ${money(total.toPlainString())}", color = Violet)
                    group.forEach { due -> DueCard(due, { details(due) }, { nudge(due) }) }
                }
            }
        } else {
            items(visible, key = { it.id }) { due -> DueCard(due, { details(due) }, { nudge(due) }) }
        }
    }
}
