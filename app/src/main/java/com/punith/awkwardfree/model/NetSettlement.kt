package com.punith.awkwardfree.model

import java.math.BigDecimal
import java.util.Locale

/** Offsets equal amounts in both directions, oldest due first. This does not transfer money. */
internal fun offsetFriendTabs(reference: Due, dues: List<Due>): List<Due> {
    val phone = normalizePhone(reference.phone) ?: return dues
    val name = reference.name.trim().lowercase(Locale.ROOT)
    val matching = dues.filter {
        !it.settled && normalizePhone(it.phone) == phone &&
            it.name.trim().lowercase(Locale.ROOT) == name
    }
    val receivable = matching.filterNot { it.owedByMe }.sumOf { it.remaining() }
    val payable = matching.filter { it.owedByMe }.sumOf { it.remaining() }
    val offset = receivable.min(payable)
    if (offset <= BigDecimal.ZERO) return dues
    val changed = mutableMapOf<String, Due>()
    for (direction in listOf(false, true)) {
        var left = offset
        for (due in matching.filter { it.owedByMe == direction }.sortedWith(compareBy<Due> { it.date }.thenBy { it.id })) {
            if (left <= BigDecimal.ZERO) break
            val amount = left.min(due.remaining())
            if (amount > BigDecimal.ZERO) {
                changed[due.id] = due.recordPayment(amount)
                left -= amount
            }
        }
    }
    return dues.map { changed[it.id] ?: it }
}
