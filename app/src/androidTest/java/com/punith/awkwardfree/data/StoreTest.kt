package com.punith.awkwardfree.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.Context
import com.punith.awkwardfree.model.Due
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal

@RunWith(AndroidJUnit4::class)
class StoreTest {
    private lateinit var context: Context
    private lateinit var store: Store
    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("awkward_free", 0).edit().clear().commit()
        store = Store(context)
    }
    @After fun tearDown() { context.getSharedPreferences("awkward_free", 0).edit().clear().commit() }
    @Test fun legacyEntriesLoadWithZeroPaid() {
        val legacy = """[{"id":"1","name":"Ananya","amount":"500.00","reason":"Dinner","date":"2026-11-24","phone":"","settled":false,"lastNudged":0}]"""
        context.getSharedPreferences("awkward_free", 0).edit().putString("dues", legacy).commit()
        val due = store.read().single()
        assertEquals(0, due.remaining().compareTo(BigDecimal("500.00")))
    }
    @Test fun backupRoundTripPreservesPartialPaymentsAndSettings() {
        val due = Due(name = "Ananya", amount = "500.00", reason = "Dinner", date = "2026-11-24").recordPayment(BigDecimal("300.00"))
        store.write(listOf(due)); store.upi = "punith@upi"; store.displayName = "Punith"
        val backup = store.exportBackup()
        store.write(emptyList()); store.upi = ""; store.displayName = ""
        assertTrue(store.restore(store.parseBackup(backup)))
        assertEquals("punith@upi", store.upi)
        assertEquals("Punith", store.displayName)
        assertEquals(0, store.read().single().remaining().compareTo(BigDecimal("200.00")))
    }
    @Test fun accountsAndExistingDeviceDataAreIsolated() {
        val guest = Due(name = "Guest", amount = "50.00", reason = "Lunch", date = "2026-11-24")
        val alice = Due(name = "Alice", amount = "500.00", reason = "Dinner", date = "2026-11-24")
        store.write(listOf(guest))
        val first = Store(context, "account-a")
        val second = Store(context, "account-b")
        first.write(listOf(alice))
        assertEquals(guest.id, store.read().single().id)
        assertEquals(alice.id, first.read().single().id)
        assertTrue(second.read().isEmpty())
        context.getSharedPreferences("awkward_free_user_account-a", 0).edit().clear().commit()
    }
    @Test fun debtDirectionSurvivesBackupAndLegacyDefaultsToOwedToMe() {
        val outgoing = Due(name = "Rahul", amount = "200.00", reason = "Lunch", date = "2026-11-24", owedByMe = true)
        store.write(listOf(outgoing))
        val exported = store.exportBackup()
        store.write(emptyList())
        assertTrue(store.restore(store.parseBackup(exported)))
        assertTrue(store.read().single().owedByMe)

        val legacy = """{"version":1,"dues":[{"id":"1","name":"Ananya","amount":"500.00","reason":"Dinner","date":"2026-11-24"}],"upi":"","displayName":""}"""
        assertFalse(store.parseBackup(legacy).dues.single().owedByMe)
    }
    @Test fun reminderHistorySurvivesBackupRestore() {
        val due = Due(name = "Ananya", amount = "500.00", reason = "Dinner", date = "2026-11-24")
            .recordNudgeOpened(100L).recordNudgeOpened(200L)
        store.write(listOf(due))
        val backup = store.parseBackup(store.exportBackup())
        store.write(emptyList())
        assertTrue(store.restore(backup))
        assertEquals(listOf(100L, 200L), store.read().single().nudgeHistory)
    }
    @Test fun invalidBackupDoesNotOverwriteCurrentRecords() {
        val due = Due(name = "Ananya", amount = "500.00", reason = "Dinner", date = "2026-11-24")
        store.write(listOf(due))
        try { store.parseBackup("""{"version":999,"dues":[],"upi":"","displayName":""}"""); fail("Invalid version should fail") } catch (_: IllegalArgumentException) { }
        assertEquals(due.id, store.read().single().id)
    }
}
