package com.punith.awkwardfree.util

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.punith.awkwardfree.model.Due
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal

@RunWith(AndroidJUnit4::class)
class ReminderTest {
    @Test fun reminderAndUpiUseOutstandingAmount() {
        val due = Due(name = "Ananya", amount = "500.00", reason = "Dinner", date = "2026-11-24").recordPayment(BigDecimal("300.00"))
        val uri = Uri.parse(upiLink("punith@upi", due))
        assertEquals("punith@upi", uri.getQueryParameter("pa"))
        assertEquals("200.00", uri.getQueryParameter("am"))
        assertEquals("Dinner", uri.getQueryParameter("tn"))
        assertTrue(message(due, "punith@upi", "Follow-up").contains("₹200"))
    }
}
