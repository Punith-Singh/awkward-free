package com.punith.awkwardfree.model

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import java.util.UUID

internal data class Due(
    val id: String = UUID.randomUUID().toString(), val name: String, val amount: String,
    val reason: String, val date: String, val phone: String = "", val settled: Boolean = false,
    val lastNudged: Long = 0L
)
