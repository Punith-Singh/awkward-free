package com.punith.awkwardfree.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.punith.awkwardfree.model.*
import com.punith.awkwardfree.ui.components.*
import com.punith.awkwardfree.ui.theme.*

@Composable internal fun ListScreen(title: String, subtitle: String, dues: List<Due>, settled: Boolean, back: () -> Unit, details: (Due) -> Unit, nudge: (Due) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(22.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
        item { BackHeader(if (settled) "Settled" else "Your tabs", back) }
        item { Heading(if (settled) "GOOD FRIENDS. CLEAR TABS." else "KEEP TRACK", title, subtitle) }
        item { Pill("${dues.size} ${if (settled) "SETTLED" else "OPEN"}", Lavender, Violet) }
        if (dues.isEmpty()) item { EmptyCard(if (settled) "No settled tabs yet" else "No open tabs", if (settled) "Tabs you mark as settled will appear here." else "Add a due to get started.") }
        items(dues, key = { it.id }) { d -> DueCard(d, { details(d) }, { nudge(d) }) }
    }
}
