package com.punith.awkwardfree.model

import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class SplitSharesTest {
    @Test fun percentageRoundingPreservesEveryPaisa() {
        val shares = splitShares(BigDecimal("10.00"), "Percent", listOf("33.33", "33.33", "33.34"))
        assertEquals(listOf(BigDecimal("3.33"), BigDecimal("3.33"), BigDecimal("3.34")), shares)
        assertEquals(BigDecimal("10.00"), shares.reduce(BigDecimal::add))
    }
    @Test fun customAmountsMustMatchTotal() {
        assertEquals(listOf(BigDecimal("3.00"), BigDecimal("7.00")), splitShares(BigDecimal("10.00"), "Amount", listOf("3", "7")))
        assertThrows(IllegalArgumentException::class.java) { splitShares(BigDecimal("10.00"), "Amount", listOf("3", "6")) }
        assertThrows(IllegalArgumentException::class.java) { splitShares(BigDecimal("10.00"), "Percent", listOf("40", "50")) }
    }
    @Test fun equalSplitIncludesEveryParticipant() {
        assertEquals(BigDecimal("10.00"), splitShares(BigDecimal("10.00"), "Equal", listOf("", "", "")).reduce(BigDecimal::add))
    }
}
