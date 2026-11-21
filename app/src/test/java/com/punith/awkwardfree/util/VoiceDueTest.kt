package com.punith.awkwardfree.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VoiceDueTest {
    @Test fun extractsBorrowedDetails() {
        assertEquals(VoiceDue("Rahul", "1500", "lunch"), parseVoiceDue("Rahul borrowed 1,500 rupees for lunch"))
    }

    @Test fun extractsLentDetails() {
        assertEquals(VoiceDue("Priya", "250.50", "tickets"), parseVoiceDue("I gave Priya 250.50 for tickets"))
    }

    @Test fun leavesUnrecognizedFieldsEmpty() {
        val result = parseVoiceDue("Someone needs to pay me back")
        assertNull(result.name)
        assertNull(result.amount)
        assertNull(result.reason)
    }
}
