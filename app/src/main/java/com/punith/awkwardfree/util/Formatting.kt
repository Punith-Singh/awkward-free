package com.punith.awkwardfree.util

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

internal fun money(amount: String): String = "₹" + try {
    java.text.NumberFormat.getNumberInstance(Locale("en", "IN")).apply { maximumFractionDigits = 2; minimumFractionDigits = 0 }.format(BigDecimal(amount))
} catch (_: Exception) { amount }
internal fun dateLabel(date: String): String = try { LocalDate.parse(date).format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)) } catch (_: Exception) { date }
internal fun nudgeLabel(time: Long): String = if (time == 0L) "Not nudged yet" else "Opened " + java.text.SimpleDateFormat("d MMM, h:mm a", Locale.getDefault()).format(java.util.Date(time))
