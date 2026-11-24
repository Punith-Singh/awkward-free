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
        java.io.File(context.filesDir, "auto_backup").deleteRecursively()
        java.io.File(context.noBackupFilesDir, "app_backup_migrated").delete()
        context.getSharedPreferences("awkward_free", 0).edit().clear().commit()
        store = Store(context)
    }
    @After fun tearDown() { context.getSharedPreferences("awkward_free", 0).edit().clear().commit(); java.io.File(context.filesDir, "auto_backup").deleteRecursively(); java.io.File(context.noBackupFilesDir, "app_backup_migrated").delete() }
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
    @Test fun monthlyCursorSurvivesBackupRestore() {
        val due = Due(name = "Rahul", amount = "500.00", reason = "Rent", date = "2026-01-31",
            repeatMonthly = true, recurrenceThrough = "2026-02-28")
        store.write(listOf(due))
        val backup = store.parseBackup(store.exportBackup())
        store.write(emptyList())
        assertTrue(store.restore(backup))
        assertTrue(store.read().single().repeatMonthly)
        assertEquals("2026-02-28", store.read().single().recurrenceThrough)
    }
    @Test fun splitGroupSurvivesBackupRestoreAndOldDataDefaultsEmpty() {
        val due = Due(name = "Ananya", amount = "200.00", reason = "Trip", date = "2026-11-24", splitGroupId = "trip-group")
        store.write(listOf(due))
        val backup = store.parseBackup(store.exportBackup())
        store.write(emptyList())
        assertTrue(store.restore(backup))
        assertEquals("trip-group", store.read().single().splitGroupId)
        val legacy = """{"version":4,"dues":[{"id":"1","name":"Ananya","amount":"500.00","reason":"Dinner","date":"2026-11-24"}],"upi":"","displayName":""}"""
        assertEquals("", store.parseBackup(legacy).dues.single().splitGroupId)
    }
    @Test fun restoredSnapshotsAreIsolatedByAccount() {
        val guest = Due(name = "Guest", amount = "50.00", reason = "Lunch", date = "2026-11-24")
        val alice = Due(name = "Alice", amount = "100.00", reason = "Dinner", date = "2026-11-24")
        store.write(listOf(guest)); store.displayName = "Guest"
        val account = Store(context, "restore-test")
        account.write(listOf(alice)); account.upi = "alice@upi"
        context.getSharedPreferences("awkward_free", 0).edit().clear().commit()
        context.getSharedPreferences("awkward_free_user_restore-test", 0).edit().clear().commit()
        assertEquals(guest.id, Store(context).read().single().id)
        val restored = Store(context, "restore-test")
        assertEquals(alice.id, restored.read().single().id)
        assertEquals("alice@upi", restored.upi)
        context.getSharedPreferences("awkward_free_user_restore-test", 0).edit().clear().commit()
    }
    @Test fun invalidDataDoesNotReplaceGoodSnapshot() {
        val due = Due(name = "Ananya", amount = "50.00", reason = "Lunch", date = "2026-11-24")
        store.write(listOf(due))
        context.getSharedPreferences("awkward_free", 0).edit().putString("dues", "broken").commit()
        Store(context)
        context.getSharedPreferences("awkward_free", 0).edit().clear().commit()
        assertEquals(due.id, Store(context).read().single().id)
    }
    @Test fun invalidBackupDoesNotOverwriteCurrentRecords() {
        val due = Due(name = "Ananya", amount = "500.00", reason = "Dinner", date = "2026-11-24")
        store.write(listOf(due))
        try { store.parseBackup("""{"version":999,"dues":[],"upi":"","displayName":""}"""); fail("Invalid version should fail") } catch (_: IllegalArgumentException) { }
        assertEquals(due.id, store.read().single().id)
    }
}
