package com.punith.awkwardfree.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.punith.awkwardfree.model.*

@Composable
internal fun DuesScreen(dues: List<Due>, back: () -> Unit, details: (Due) -> Unit, nudge: (Due) -> Unit) {
    ListScreen("All dues", "Everything that’s still open.", dues, false, back, details, nudge)
}
