package com.punith.awkwardfree.model

import org.junit.Assert.*
import org.junit.Test

class NetSettlementTest {
    private fun tab(id: String, amount: String, outgoing: Boolean, date: String = "2026-11-22", phone: String = "9845012345") =
        Due(id = id, name = "Rahul", phone = phone, amount = amount, reason = "Food", date = date, owedByMe = outgoing)

    @Test fun offsetsEqualAmountsInBothDirectionsOldestFirst() {
        val old = tab("old", "200.00", false, "2026-11-01")
        val later = tab("later", "300.00", false, "2026-11-02")
        val payable = tab("pay", "350.00", true, phone = "+91 98450 12345")
        val other = tab("other", "100.00", true, phone = "9999999999")
        val result = offsetFriendTabs(old, listOf(old, later, payable, other)).associateBy { it.id }
        assertTrue(result.getValue("old").settled)
        assertEquals("150.00", result.getValue("later").paidAmount)
        assertTrue(result.getValue("pay").settled)
        assertEquals("0.00", result.getValue("other").paidAmount)
        assertEquals(0, friendBalance(result.getValue("later"), result.values.toList())!!.net.compareTo(java.math.BigDecimal("150")))
    }

    @Test fun missingPhoneOrOneDirectionChangesNothing() {
        val noPhone = tab("a", "100.00", false, phone = "")
        val outgoing = tab("b", "50.00", true, phone = "")
        val original = listOf(noPhone, outgoing)
        assertEquals(original, offsetFriendTabs(noPhone, original))
        val one = listOf(tab("c", "20.00", false))
        assertEquals(one, offsetFriendTabs(one.first(), one))
    }
}
