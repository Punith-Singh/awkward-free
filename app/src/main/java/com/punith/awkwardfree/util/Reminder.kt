package com.punith.awkwardfree.util

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.punith.awkwardfree.model.*

internal fun upiLink(upi: String, due: Due): String = Uri.Builder().scheme("upi").authority("pay").appendQueryParameter("pa", upi).appendQueryParameter("am", due.amount).appendQueryParameter("cu", "INR").appendQueryParameter("tn", due.reason).build().toString()
internal fun message(d: Due, upi: String, tone: String): String {
    val intro = when (tone) {
        "Playful" -> "Hey ${d.name}! My UPI tab says it misses ${money(d.amount)} 😄 Mind settling up for ${d.reason} when you get a chance?"
        "Direct" -> "Hi ${d.name}, could you please send me ${money(d.amount)} for ${d.reason}? Thanks!"
        else -> "Hey ${d.name}! Just a gentle reminder about ${money(d.amount)} for ${d.reason}. Whenever you get a chance, could you send it over? Thanks!"
    }
    return intro
}
