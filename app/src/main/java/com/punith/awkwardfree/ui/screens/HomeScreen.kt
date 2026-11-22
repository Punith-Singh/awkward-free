package com.punith.awkwardfree.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.punith.awkwardfree.model.*
import com.punith.awkwardfree.ui.components.*
import com.punith.awkwardfree.ui.theme.*
import com.punith.awkwardfree.util.*
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Locale

@Composable internal fun BalanceCard(dues: List<Due>) {
    val open = dues.filterNot { it.settled }; val total = open.filterNot { it.owedByMe }.fold(BigDecimal.ZERO) { acc, d -> acc + d.remaining() }; val owed = open.filter { it.owedByMe }.fold(BigDecimal.ZERO) { acc, d -> acc + d.remaining() }
    Column(Modifier.fillMaxWidth().background(Deep, RoundedCornerShape(24.dp)).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Pill("YOUR BALANCE", Color(0xFF4D427C), Color(0xFFE4DFFF)); Text("✦", color = Lime, fontSize = 21.sp) }
        Text("You’re owed", color = Color(0xFFC8C2E0), fontSize = 13.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(money(total.toPlainString()), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 33.sp); Pill("${open.size} OPEN", Lime, Deep) }
        Text("You owe ${money(owed.toPlainString())} · Net ${money((total - owed).toPlainString())}", color = Color(0xFFD9D3EE), fontSize = 12.sp)
    }
}
@Composable internal fun HomeScreen(dues: List<Due>, name: String, all: () -> Unit, settled: () -> Unit, add: () -> Unit, split: () -> Unit, details: (Due) -> Unit, nudge: (Due) -> Unit) {
    val open = dues.filterNot { it.settled }
    val today = LocalDate.now()
    fun dateOf(due: Due): LocalDate? = runCatching { LocalDate.parse(due.date) }.getOrNull()
    val overdue = open.filter { dateOf(it)?.isBefore(today) == true }
    val upcoming = open.filter { dateOf(it)?.let { date -> !date.isBefore(today) && !date.isAfter(today.plusDays(7)) } == true }
    val priority = open.sortedWith(compareBy<Due> { dateOf(it) ?: LocalDate.MAX }.thenBy { it.id })
    val largestGroup = open.filterNot { it.owedByMe }.groupBy {
        normalizePhone(it.phone)?.let { phone -> "${it.name.trim().lowercase(Locale.ROOT)}:$phone" } ?: "entry:${it.id}"
    }.values.maxByOrNull { group -> group.fold(BigDecimal.ZERO) { sum, d -> sum + d.remaining() } }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Column { Text("GOOD TO SEE YOU", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(5.dp)); Text(if (name.isBlank()) "Hey there ✨" else "Hey, ${name.trim()} ✨", color = Ink, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold) }; Box(Modifier.size(40.dp).background(Violet, RoundedCornerShape(15.dp)), contentAlignment = Alignment.Center) { Text(name.trim().take(1).uppercase().ifEmpty { "✦" }, color = Color.White, fontWeight = FontWeight.Bold) } } }
        item { BalanceCard(dues) }
        if (open.isNotEmpty()) item {
            Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(20.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Coming up", color = Ink, fontWeight = FontWeight.Bold)
                Text("${overdue.size} overdue · ${upcoming.size} due in the next 7 days", color = if (overdue.isNotEmpty()) MaterialTheme.colorScheme.error else Muted)
                largestGroup?.let { group ->
                    val total = group.fold(BigDecimal.ZERO) { sum, due -> sum + due.remaining() }
                    Text("Largest owed to you: ${group.first().name} · ${money(total.toPlainString())}", color = Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("Your tabs", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Ink); TextButton(onClick = all) { Text("See all", fontWeight = FontWeight.Bold) } } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Pill("Open tabs  ${open.size}", Ink, Color.White, all); Pill("Settled  ${dues.count { it.settled }}", Color.White, Muted, settled) } }
        if (open.isEmpty()) item { EmptyCard("Nothing to chase ✨", "Add a due and make asking easier.") }
        items(priority.take(3), key = { it.id }) { d ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (dateOf(d)?.isBefore(today) == true) Text("OVERDUE", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                else if (dateOf(d)?.let { !it.isAfter(today.plusDays(7)) } == true) Text("DUE THIS WEEK", color = Violet, style = MaterialTheme.typography.labelSmall)
                DueCard(d, { details(d) }, { nudge(d) })
            }
        }
        item { PrimaryButton("＋   Add a due", add) }
        item { OutlinedButton(onClick = split, modifier = Modifier.fillMaxWidth()) { Text("Split an expense") } }
    }
}
