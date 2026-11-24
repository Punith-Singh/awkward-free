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
import androidx.compose.ui.unit.sp
import com.punith.awkwardfree.model.Due
import com.punith.awkwardfree.ui.components.*
import com.punith.awkwardfree.ui.theme.*
import java.time.LocalDate

@Composable internal fun DuesDashboard(
    dues: List<Due>, friends: () -> Unit, details: (Due) -> Unit, nudge: (Due) -> Unit
) {
    var filter by remember { mutableStateOf("Open") }
    var query by remember { mutableStateOf("") }
    val today = LocalDate.now()
    val overdue = dues.count { !it.settled && runCatching { LocalDate.parse(it.date).isBefore(today) }.getOrDefault(false) }
    val visible = dues.filter { due ->
        val matches = query.isBlank() || due.name.contains(query.trim(), true) || due.reason.contains(query.trim(), true)
        matches && when (filter) {
            "Settled" -> due.settled
            "Overdue" -> !due.settled && runCatching { LocalDate.parse(due.date).isBefore(today) }.getOrDefault(false)
            else -> !due.settled
        }
    }.sortedBy { it.date }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 20.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text("Dues", color = Ink, fontSize = 29.sp, fontWeight = FontWeight.ExtraBold) }
        item {
            Column(Modifier.fillMaxWidth().background(Deep, RoundedCornerShape(24.dp)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BalanceCardContents(dues, overdue)
            }
        }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Open", "Overdue", "Settled").forEach { option ->
                    FilterChip(selected = filter == option, onClick = { filter = option }, label = { Text(option) })
                }
                FilterChip(selected = false, onClick = friends, label = { Text("By friend") })
            }
        }
        item { OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Search name or reason") }) }
        if (visible.isEmpty()) item { EmptyCard("No matching tabs", "Your tabs will appear here.") }
        items(visible, key = { it.id }) { due -> DueCard(due, { details(due) }, { nudge(due) }) }
    }
}

@Composable private fun BalanceCardContents(dues: List<Due>, overdue: Int) {
    val open = dues.filterNot { it.settled }
    val received = open.filterNot { it.owedByMe }.fold(java.math.BigDecimal.ZERO) { sum, due -> sum + due.remaining() }
    val owed = open.filter { it.owedByMe }.fold(java.math.BigDecimal.ZERO) { sum, due -> sum + due.remaining() }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("YOU’RE OWED", color = Color(0xFFC8C2E0), style = MaterialTheme.typography.labelMedium)
        if (overdue > 0) Text("$overdue overdue", color = Lime, style = MaterialTheme.typography.labelMedium)
    }
    Text(com.punith.awkwardfree.util.money(received.toPlainString()), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp)
    Text("You owe ${com.punith.awkwardfree.util.money(owed.toPlainString())} · Net ${com.punith.awkwardfree.util.money((received - owed).toPlainString())}", color = Color(0xFFD9D3EE), style = MaterialTheme.typography.bodySmall)
}
