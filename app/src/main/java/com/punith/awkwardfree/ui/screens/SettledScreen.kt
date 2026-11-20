package com.punith.awkwardfree.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.punith.awkwardfree.model.*

@Composable
internal fun SettledScreen(dues: List<Due>, back: () -> Unit, details: (Due) -> Unit) {
    ListScreen("All square.", "Your settled tabs, all in one place.", dues, true, back, details, {})
}
