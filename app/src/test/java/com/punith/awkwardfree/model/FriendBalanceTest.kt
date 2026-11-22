package com.punith.awkwardfree.model

import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class FriendBalanceTest {
    private fun tab(name: String = "Rahul", phone: String = "919999999999", amount: String = "500.00",
                    paid: String = "0.00", outgoing: Boolean = false, settled: Boolean = false) =
        Due(name = name, amount = amount, paidAmount = paid, reason = "Lunch", date = "2026-11-24",
            phone = phone, owedByMe = outgoing, settled = settled)

    @Test fun netUsesRemainingAmountsAndBothDirections() {
        val reference = tab(paid = "200.00")
        val result = friendBalance(reference, listOf(reference, tab(name = " rahul ", amount = "100.00", outgoing = true),
            tab(amount = "999.00", settled = true), tab(name = "Different", amount = "888.00")))
        assertNotNull(result)
        assertEquals(0, result!!.owedToMe.compareTo(BigDecimal("300.00")))
        assertEquals(0, result.iOwe.compareTo(BigDecimal("100.00")))
        assertEquals(0, result.net.compareTo(BigDecimal("200.00")))
    }

    @Test fun missingOrDifferentPhoneDoesNotCombinePeople() {
        assertNull(friendBalance(tab(phone = ""), listOf(tab(phone = ""))))
        val reference = tab()
        val result = friendBalance(reference, listOf(reference, tab(phone = "918888888888", outgoing = true)))
        assertEquals(0, result!!.iOwe.signum())
        assertEquals(0, result.net.compareTo(BigDecimal("500.00")))
    }
}
