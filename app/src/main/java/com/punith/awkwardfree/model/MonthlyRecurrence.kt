package com.punith.awkwardfree.model

import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

/**
 * Create due monthly occurrences through today. Source entries retain the cursor;
 * generated entries are ordinary one-off tabs. No network or background writes.
 */
internal fun materializeMonthly(dues: List<Due>, today: LocalDate): List<Due> {
    val ids = dues.mapTo(mutableSetOf()) { it.id }
    val created = mutableListOf<Due>()
    val updated = dues.map { root ->
        if (!root.repeatMonthly) return@map root
        val anchor = runCatching { LocalDate.parse(root.date) }.getOrNull() ?: return@map root
        val cursor = runCatching { LocalDate.parse(root.recurrenceThrough.ifBlank { root.date }) }.getOrNull() ?: return@map root
        var month = YearMonth.from(cursor).plusMonths(1)
        var through = cursor
        var generated = 0
        while (generated < 120 && dues.size + created.size < 10000) {
            val next = month.atDay(minOf(anchor.dayOfMonth, month.lengthOfMonth()))
            if (next.isAfter(today)) break
            val key = "awkward-free-monthly:${root.id}:$next"
            val id = UUID.nameUUIDFromBytes(key.toByteArray(Charsets.UTF_8)).toString()
            if (ids.add(id)) {
                created.add(root.copy(id = id, date = next.toString(), settled = false,
                    lastNudged = 0L, paidAmount = "0.00", nudgeHistory = emptyList(),
                    repeatMonthly = false, recurrenceThrough = ""))
            }
            through = next
            month = month.plusMonths(1)
            generated++
        }
        if (through != cursor) root.copy(recurrenceThrough = through.toString()) else root
    }
    return if (created.isEmpty() && updated == dues) dues else created.asReversed() + updated
}
