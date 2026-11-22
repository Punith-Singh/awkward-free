package com.punith.awkwardfree.model

import java.math.BigDecimal
import java.util.Locale

internal data class FriendBalance(val owedToMe: BigDecimal, val iOwe: BigDecimal) {
    val net: BigDecimal get() = owedToMe - iOwe
}

/** Avoid merging unverified people: match both normalized name and phone. */
internal fun friendBalance(reference: Due, all: List<Due>): FriendBalance? {
    val phone = reference.phone.trim()
    if (phone.isEmpty()) return null
    val name = reference.name.trim().lowercase(Locale.ROOT)
    val matches = all.filter {
        !it.settled && it.phone.trim() == phone && it.name.trim().lowercase(Locale.ROOT) == name
    }
    return FriendBalance(
        matches.filterNot { it.owedByMe }.fold(BigDecimal.ZERO) { sum, due -> sum + due.remaining() },
        matches.filter { it.owedByMe }.fold(BigDecimal.ZERO) { sum, due -> sum + due.remaining() }
    )
}
