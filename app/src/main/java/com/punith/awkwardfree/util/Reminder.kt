package com.punith.awkwardfree.util

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.punith.awkwardfree.model.*

internal fun upiLink(upi: String, due: Due): String = Uri.Builder().scheme("upi").authority("pay").appendQueryParameter("pa", upi).appendQueryParameter("am", due.remaining().toPlainString()).appendQueryParameter("cu", "INR").appendQueryParameter("tn", due.reason).build().toString()
internal fun message(d: Due, upi: String, tone: String): String {
    val intro = when (tone) {
        "Playful" -> "Hey ${d.name}! My UPI tab says it misses ${money(d.remaining().toPlainString())} 😄 Mind settling up for ${d.reason} when you get a chance?"
        "Super casual" -> "Hey ${d.name}! Quick reminder about ${money(d.remaining().toPlainString())} for ${d.reason} whenever you get a moment 🙌"
        "Close friend" -> "Hey ${d.name}! Can you send me ${money(d.remaining().toPlainString())} for ${d.reason} when you get a sec? Thanks 💜"
        "Follow-up" -> "Hey ${d.name}! Just checking if you had a chance to see my reminder about ${money(d.remaining().toPlainString())} for ${d.reason}. No rush, thanks!"
        "Polite follow-up" -> "Hi ${d.name}, just following up on the ${money(d.remaining().toPlainString())} for ${d.reason}. Could you please let me know when you can send it? Thanks!"
        "Direct" -> "Hi ${d.name}, could you please send me ${money(d.remaining().toPlainString())} for ${d.reason}? Thanks!"
        else -> "Hey ${d.name}! Just a gentle reminder about ${money(d.remaining().toPlainString())} for ${d.reason}. Whenever you get a chance, could you send it over? Thanks!"
    }
    return intro
}
