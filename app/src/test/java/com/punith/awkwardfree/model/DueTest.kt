package com.punith.awkwardfree.model

import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class DueTest {
    private fun due() = Due(name = "Ananya", amount = "500.00", reason = "Dinner", date = "2026-11-24")
    @Test fun partialPaymentsReduceBalanceAndFullPaymentSettles() {
        val partial = due().recordPayment(BigDecimal("300.00"))
        assertEquals(0, partial.remaining().compareTo(BigDecimal("200.00")))
        assertFalse(partial.settled)
        val paid = partial.recordPayment(BigDecimal("200.00"))
        assertEquals(0, paid.remaining().signum())
        assertTrue(paid.settled)
    }
    @Test fun invalidPaymentsAreRejected() {
        for (amount in listOf("0", "-1", "500.01", "1.001")) {
            assertThrows(IllegalArgumentException::class.java) { due().recordPayment(BigDecimal(amount)) }
        }
    }
    @Test fun existingDuesDefaultToNoPartialPayment() {
        assertEquals(0, due().remaining().compareTo(BigDecimal("500.00")))
    }
}
