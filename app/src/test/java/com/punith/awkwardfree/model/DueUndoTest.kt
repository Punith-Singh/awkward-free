package com.punith.awkwardfree.model

import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class DueUndoTest {
    private fun tab(id: String, outgoing: Boolean = false) =
        Due(id = id, name = "Rahul", phone = "919845012345", amount = "100.00", reason = "Lunch", date = "2026-11-24", owedByMe = outgoing)

    @Test fun restoresBothSidesOfAnOffsetWithoutRemovingNewTabs() {
        val incoming = tab("in")
        val outgoing = tab("out", true)
        val before = listOf(incoming, outgoing)
        val after = offsetFriendTabs(incoming, before)
        val newTab = tab("new")
        val restored = DueUndo.between(before, after)!!.restore(listOf(newTab) + after)!!
        assertEquals(listOf(newTab, incoming, outgoing), restored)
    }

    @Test fun deleteRestoresOriginalRecordWithoutRemovingOtherChanges() {
        val a = tab("a")
        val b = tab("b")
        val later = b.recordPayment(BigDecimal("10.00"))
        val undo = DueUndo.between(listOf(a, b), listOf(b))!!
        assertEquals(listOf(a, later), undo.restore(listOf(later)))
    }

    @Test fun refusesUndoIfAffectedTabChangedAfterwards() {
        val a = tab("a")
        val settled = a.recordPayment(BigDecimal("100.00"))
        val undo = DueUndo.between(listOf(a), listOf(settled))!!
        assertNull(undo.restore(listOf(settled.copy(reason = "Changed"))))
        assertNull(undo.restore(emptyList()))
    }
}
