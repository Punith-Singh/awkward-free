package com.punith.awkwardfree.model

import org.junit.Assert.*
import org.junit.Test

class PhoneNumberTest {
    @Test fun indianMobileFormatsMatch() {
        assertEquals("919845012345", normalizePhone("98450 12345"))
        assertEquals("919845012345", normalizePhone("+91 98450 12345"))
        assertEquals("919845012345", normalizePhone("919845012345"))
        assertEquals("919845012345", normalizePhone("0091-98450-12345"))
    }

    @Test fun internationalNumbersAndInvalidInput() {
        assertEquals("14155552671", normalizePhone("+1 (415) 555-2671"))
        assertNull(normalizePhone(""))
        assertNull(normalizePhone("+91 98abc45012345"))
        assertNull(normalizePhone("123"))
        assertNull(normalizePhone("91+9845012345"))
    }

    @Test fun friendBalanceMatchesLegacyFormattedNumbers() {
        val first = Due(name = "Rahul", phone = "98450 12345", amount = "500", reason = "Lunch", date = "2026-11-24")
        val second = Due(name = "rahul", phone = "+91 98450 12345", amount = "200", reason = "Taxi", date = "2026-11-24", owedByMe = true)
        assertEquals(0, friendBalance(first, listOf(first, second))!!.net.compareTo(java.math.BigDecimal("300")))
    }
}
