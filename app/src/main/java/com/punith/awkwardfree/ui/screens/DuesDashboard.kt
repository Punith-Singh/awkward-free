package com.punith.awkwardfree.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.punith.awkwardfree.model.Due
import com.punith.awkwardfree.ui.components.*
import com.punith.awkwardfree.ui.theme.*
import com.punith.awkwardfree.util.money
import java.math.BigDecimal
import java.time.LocalDate

@Composable internal fun DuesDashboard(
    dues: List<Due>, friends: () -> Unit, add: () -> Unit, details: (Due) -> Unit, nudge: (Due) -> Unit
) {
    var filter by remember { mutableStateOf("Open") }
    var query by remember { mutableStateOf("") }
    val today = LocalDate.now()
    val open = dues.filterNot { it.settled }
    val overdue = open.count { runCatching { LocalDate.parse(it.date).isBefore(today) }.getOrDefault(false) }
    val receivable = open.filterNot { it.owedByMe }.fold(BigDecimal.ZERO) { sum, due -> sum + due.remaining() }
    val payable = open.filter { it.owedByMe }.fold(BigDecimal.ZERO) { sum, due -> sum + due.remaining() }
    val visible = dues.filter { due ->
        val matches = query.isBlank() || due.name.contains(query.trim(), true) || due.reason.contains(query.trim(), true)
        matches && when (filter) {
            "Settled" -> due.settled
            "Overdue" -> !due.settled && runCatching { LocalDate.parse(due.date).isBefore(today) }.getOrDefault(false)
            else -> !due.settled
        }
    }.sortedBy { it.date }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 112.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("AWKWARD-FREE", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                    Text("Your tabs", color = Ink, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                }
                Box(Modifier.size(42.dp).background(Lavender, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.ReceiptLong, contentDescription = null, tint = Violet, modifier = Modifier.size(21.dp))
                }
            }
        }
        item {
            Column(
                Modifier.fillMaxWidth().shadow(12.dp, RoundedCornerShape(26.dp), ambientColor = Deep.copy(alpha = .22f), spotColor = Deep.copy(alpha = .28f)).background(Brush.linearGradient(listOf(Deep, HeroEnd)), RoundedCornerShape(26.dp)).padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("TOTAL OWED TO YOU", color = Color(0xFFC8C2E0), style = MaterialTheme.typography.labelMedium)
                    if (overdue > 0) Text("$overdue overdue", color = Deep, fontWeight = FontWeight.Bold, fontSize = 11.sp,
                        modifier = Modifier.background(Lime, CircleShape).padding(horizontal = 10.dp, vertical = 6.dp))
                }
                Text(money(receivable.toPlainString()), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 36.sp)
                HorizontalDivider(color = Color.White.copy(alpha = .16f))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("YOU OWE", color = Color(0xFFC8C2E0), style = MaterialTheme.typography.labelSmall)
                        Text(money(payable.toPlainString()), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("NET BALANCE", color = Color(0xFFC8C2E0), style = MaterialTheme.typography.labelSmall)
                        Text(money((receivable - payable).toPlainString()), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("OPEN", color = Color(0xFFC8C2E0), style = MaterialTheme.typography.labelSmall)
                        Text(open.size.toString(), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Your activity", color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp)
                TextButton(onClick = friends) { Text("By friend") }
            }
        }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Open", "Overdue", "Settled").forEach { option ->
                    FilterChip(selected = filter == option, onClick = { filter = option }, label = { Text("$option  ${when (option) { "Open" -> open.size; "Overdue" -> overdue; else -> dues.count { it.settled } }}") },
                        shape = RoundedCornerShape(50), border = null,
                        colors = FilterChipDefaults.filterChipColors(containerColor = Color.White, labelColor = Muted,
                            selectedContainerColor = Deep, selectedLabelColor = Color.White))
                }
            }
        }
        if (dues.isNotEmpty() || query.isNotBlank()) item {
            OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = Muted) },
                placeholder = { Text("Search name or reason") }, label = { Text("Search tabs") },
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color.White, focusedContainerColor = Color.White,
                    unfocusedBorderColor = Color.Transparent, focusedBorderColor = Violet))
        }
        if (visible.isEmpty()) item {
            Column(Modifier.fillMaxWidth().softCard(RoundedCornerShape(24.dp), 2.dp).padding(horizontal = 22.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(58.dp).background(Lavender, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.ReceiptLong, contentDescription = null, tint = Violet, modifier = Modifier.size(26.dp))
                }
                val emptyTitle = when {
                    dues.isEmpty() -> "Good friends. Clear tabs."
                    query.isNotBlank() -> "No matching tabs"
                    filter == "Overdue" -> "Nothing overdue"
                    filter == "Settled" -> "No settled tabs yet"
                    else -> "You’re all caught up"
                }
                val emptyText = if (dues.isEmpty()) "Add your first tab and keep the little things easy." else "Try another filter or search."
                Text(emptyTitle, color = Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(emptyText, color = Muted, style = MaterialTheme.typography.bodySmall)
                if (dues.isEmpty()) TextButton(onClick = add) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp)); Text("Add your first tab")
                }
            }
        }
        items(visible, key = { it.id }) { due -> DueCard(due, { details(due) }, { nudge(due) }) }
    }
}
