package com.punith.awkwardfree.reminders

import org.junit.Assert.*
import org.junit.Test
import java.time.ZonedDateTime

class FollowUpPolicyTest {
    @Test fun movesLateAndEarlyTimesToEightAm() {
        assertEquals(ZonedDateTime.parse("2026-11-25T08:00:00+05:30[Asia/Kolkata]"), outsideQuietHours(ZonedDateTime.parse("2026-11-24T22:15:00+05:30[Asia/Kolkata]"), true))
        assertEquals(ZonedDateTime.parse("2026-11-24T08:00:00+05:30[Asia/Kolkata]"), outsideQuietHours(ZonedDateTime.parse("2026-11-24T07:30:00+05:30[Asia/Kolkata]"), true))
    }
    @Test fun allowsDaytimeOrDisabledQuietHours() {
        val day = ZonedDateTime.parse("2026-11-24T08:00:00+05:30[Asia/Kolkata]")
        val night = ZonedDateTime.parse("2026-11-24T23:00:00+05:30[Asia/Kolkata]")
        assertEquals(day, outsideQuietHours(day, true))
        assertEquals(night, outsideQuietHours(night, false))
    }
    @Test fun suggestsToneBasedOnOpenCount() {
        assertEquals("Friendly", suggestedFollowUpTone(0))
        assertEquals("Follow-up", suggestedFollowUpTone(1))
        assertEquals("Polite follow-up", suggestedFollowUpTone(2))
        assertEquals("Direct", suggestedFollowUpTone(3))
    }
}
