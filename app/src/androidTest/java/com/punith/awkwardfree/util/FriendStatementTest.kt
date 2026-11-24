package com.punith.awkwardfree.util

import android.content.Context
import android.graphics.pdf.PdfRenderer
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.punith.awkwardfree.model.Due
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FriendStatementTest {
    @Test fun createsShareablePdfForManyOpenTabs() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dues = (1..70).map { i ->
            Due(name = "Rahul", amount = "100.00", reason = "Expense $i", date = "2026-11-24", owedByMe = i % 2 == 0)
        }
        val uri = createFriendStatement(context, dues)
        assertEquals("content", uri.scheme)
        context.contentResolver.openFileDescriptor(uri, "r")!!.use { descriptor ->
            PdfRenderer(descriptor).use { renderer -> assertTrue(renderer.pageCount > 1) }
        }
    }
}
