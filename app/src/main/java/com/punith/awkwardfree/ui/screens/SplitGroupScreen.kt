package com.punith.awkwardfree.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.punith.awkwardfree.model.Due
import com.punith.awkwardfree.ui.components.BackHeader
import com.punith.awkwardfree.ui.components.DueCard
import com.punith.awkwardfree.ui.components.EmptyCard
import com.punith.awkwardfree.ui.components.Heading

@Composable internal fun SplitGroupScreen(dues: List<Due>, back: () -> Unit, details: (Due) -> Unit, nudge: (Due) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(22.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
        item { BackHeader("Split expense", back) }
        item { Heading("SPLIT EXPENSE", dues.firstOrNull()?.reason ?: "Split tabs", "All tracked tabs in this split, including settled tabs.") }
        if (dues.isEmpty()) item { EmptyCard("No tabs in this split", "These tabs may have been deleted.") }
        items(dues, key = { it.id }) { due -> DueCard(due, { details(due) }, { nudge(due) }) }
    }
}
