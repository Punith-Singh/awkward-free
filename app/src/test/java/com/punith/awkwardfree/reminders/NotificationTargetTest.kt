package com.punith.awkwardfree.reminders

import com.punith.awkwardfree.model.Due
import org.junit.Assert.*
import org.junit.Test

class NotificationTargetTest {
    private val due = Due(id = "1", name = "Rahul", amount = "50.00", reason = "Lunch", date = "2026-11-24")

    @Test fun wrongAccountOrSettledOrMissingTabDoesNotOpen() {
        val target = NotificationTarget("1", "account-a", true)
        assertNull(resolveNotificationTarget(target, "account-b", listOf(due)))
        assertNull(resolveNotificationTarget(target, "account-a", listOf(due.copy(settled = true))))
        assertNull(resolveNotificationTarget(target, "account-a", emptyList()))
    }

    @Test fun outgoingDebtNeverOpensANudge() {
        val target = NotificationTarget("1", "", true)
        assertEquals("details", resolveNotificationTarget(target, "", listOf(due.copy(owedByMe = true)))?.second)
        assertEquals("nudge", resolveNotificationTarget(target, "", listOf(due))?.second)
        assertEquals("details", resolveNotificationTarget(target.copy(openNudge = false), "", listOf(due))?.second)
    }
}
