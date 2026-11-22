package com.punith.awkwardfree.model

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class MonthlyRecurrenceTest {
    private fun monthly() = Due(id = "root", name = "Rahul", amount = "500.00", reason = "Rent",
        date = "2026-01-31", repeatMonthly = true, recurrenceThrough = "2026-01-31")

    @Test fun monthEndsAreClampedWithoutDriftingAndGenerationIsIdempotent() {
        val first = materializeMonthly(listOf(monthly()), LocalDate.parse("2026-03-31"))
        assertEquals(setOf("2026-01-31", "2026-02-28", "2026-03-31"), first.map { it.date }.toSet())
        assertEquals(1, first.count { it.repeatMonthly })
        assertEquals("2026-03-31", first.single { it.repeatMonthly }.recurrenceThrough)
        assertEquals(first, materializeMonthly(first, LocalDate.parse("2026-03-31")))
    }

    @Test fun doesNotCreateFutureOrDuplicateOccurrences() {
        val february = materializeMonthly(listOf(monthly()), LocalDate.parse("2026-02-28"))
        val sourceWithOldCursor = february.map { if (it.repeatMonthly) it.copy(recurrenceThrough = "2026-01-31") else it }
        val again = materializeMonthly(sourceWithOldCursor, LocalDate.parse("2026-02-28"))
        assertEquals(2, again.size)
        assertEquals("2026-02-28", again.single { it.repeatMonthly }.recurrenceThrough)
    }

    @Test fun stoppingAndResumingSkipsPausedMonths() {
        val active = monthly().copy(recurrenceThrough = "2026-02-28")
        val stopped = active.copy(repeatMonthly = false, recurrenceThrough =
            recurrenceCursorForSave(active, LocalDate.parse(active.date), false, LocalDate.parse("2026-03-01")))
        assertEquals("2026-02-28", stopped.recurrenceThrough)
        val resumedCursor = recurrenceCursorForSave(stopped, LocalDate.parse(stopped.date), true, LocalDate.parse("2026-05-10"))
        val resumed = stopped.copy(repeatMonthly = true, recurrenceThrough = resumedCursor)
        assertEquals(listOf(resumed), materializeMonthly(listOf(resumed), LocalDate.parse("2026-05-31")))
        assertEquals(setOf("2026-01-31", "2026-06-30"), materializeMonthly(listOf(resumed), LocalDate.parse("2026-06-30")).map { it.date }.toSet())
    }

    @Test fun pausedAndLegacyTabsDoNotGenerate() {
        val original = monthly().copy(repeatMonthly = false)
        assertEquals(listOf(original), materializeMonthly(listOf(original), LocalDate.parse("2028-01-01")))
        val legacy = Due(name = "A", amount = "1.00", reason = "Lunch", date = "2026-01-01")
        assertEquals(listOf(legacy), materializeMonthly(listOf(legacy), LocalDate.parse("2028-01-01")))
    }
}
