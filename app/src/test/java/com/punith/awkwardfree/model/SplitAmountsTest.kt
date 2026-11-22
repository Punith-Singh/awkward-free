package com.punith.awkwardfree.model

import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class SplitAmountsTest {
    @Test fun sharesAddToTotalWhenPaiseDoNotDivideEvenly() {
        val shares = splitEqually(BigDecimal("100.00"), 3)
        assertEquals(listOf(BigDecimal("33.34"), BigDecimal("33.33"), BigDecimal("33.33")), shares)
        assertEquals(BigDecimal("100.00"), shares.reduce(BigDecimal::add))
    }
    @Test fun rejectsZeroSharesAndExcessPrecision() {
        assertThrows(IllegalArgumentException::class.java) { splitEqually(BigDecimal("0.01"), 2) }
        assertThrows(IllegalArgumentException::class.java) { splitEqually(BigDecimal("1.001"), 2) }
    }
}
