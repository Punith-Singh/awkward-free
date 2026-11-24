package com.punith.awkwardfree.util

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.punith.awkwardfree.model.Due
import java.io.File
import java.math.BigDecimal
import java.time.LocalDate

/** Creates a private cache file and returns a temporary, read-only content URI. */
internal fun createFriendStatement(context: Context, dues: List<Due>): android.net.Uri {
    require(dues.isNotEmpty() && dues.all { !it.settled })
    val folder = File(context.cacheDir, "statements").apply { mkdirs() }
    val file = File.createTempFile("statement_", ".pdf", folder)
    val pdf = PdfDocument()
    try {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        var pageNumber = 0
        var page: PdfDocument.Page? = null
        var y = 0f
        fun startPage() {
            page?.let { pdf.finishPage(it) }
            pageNumber++
            page = pdf.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
            y = 50f
            paint.color = Color.rgb(36, 29, 80)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 19f
            page!!.canvas.drawText("Awkward-Free · Friend statement", 40f, y, paint)
            y += 25f
            paint.typeface = Typeface.DEFAULT
            paint.color = Color.DKGRAY
            paint.textSize = 10f
            page!!.canvas.drawText("Open tabs · ${LocalDate.now()} · Page $pageNumber", 40f, y, paint)
            y += 25f
        }
        fun line(value: String, bold: Boolean = false) {
            val sanitized = value.replace('\n', ' ').replace('\r', ' ')
            paint.typeface = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
            paint.textSize = if (bold) 13f else 11f
            paint.color = Color.DKGRAY
            var text = sanitized
            while (text.isNotEmpty()) {
                if (y > 795f) {
                    startPage()
                    paint.typeface = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
                    paint.textSize = if (bold) 13f else 11f
                    paint.color = Color.DKGRAY
                }
                val count = paint.breakText(text, true, 510f, null).coerceAtLeast(1)
                page!!.canvas.drawText(text.substring(0, count), 40f, y, paint)
                text = text.substring(count).trimStart()
                y += 17f
            }
        }
        startPage()
        line("Friend: ${dues.first().name}", true)
        y += 8f
        dues.sortedBy { it.date }.forEach { due ->
            line("${if (due.owedByMe) "You owe" else "Owed to you"} · ${due.reason}")
            line("Due ${due.date}    Remaining: ${money(due.remaining().toPlainString())}")
            y += 8f
        }
        val received = dues.filterNot { it.owedByMe }.fold(BigDecimal.ZERO) { sum, due -> sum + due.remaining() }
        val owed = dues.filter { it.owedByMe }.fold(BigDecimal.ZERO) { sum, due -> sum + due.remaining() }
        y += 8f
        line("Owed to you: ${money(received.toPlainString())}", true)
        line("You owe: ${money(owed.toPlainString())}", true)
        val net = received - owed
        line(if (net.signum() >= 0) "Net owed to you: ${money(net.toPlainString())}" else "Net you owe: ${money(net.abs().toPlainString())}", true)
        line("This is a summary of records; it does not confirm payment.")
        page?.let { pdf.finishPage(it) }
        file.outputStream().use { pdf.writeTo(it) }
    } catch (error: Exception) {
        file.delete()
        throw error
    } finally {
        pdf.close()
    }
    return FileProvider.getUriForFile(context, "${context.packageName}.statements", file)
}
