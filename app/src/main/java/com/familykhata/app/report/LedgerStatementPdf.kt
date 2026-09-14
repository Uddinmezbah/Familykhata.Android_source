package com.familykhata.app.report

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

data class StatementPdf(val file: File, val pageCount: Int)

/** Android's text layout handles Bengali shaping and font fallback on the device. */
suspend fun writeLedgerStatementPdf(
    context: Context,
    statement: LedgerStatement,
    bangla: Boolean,
    currency: String
): StatementPdf {
    val directory = File(context.cacheDir, "ledger_statements").apply { mkdirs() }
    // Keep recently shared files available for receiving apps; remove only old cached exports.
    val oldest = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
    directory.listFiles()?.filter { it.isFile && it.lastModified() < oldest }?.forEach { it.delete() }
    val file = File.createTempFile("HisabiKhata-ledger-${statement.person.id}-", ".pdf", directory)
    val document = PdfDocument()
    var activePage: PdfDocument.Page? = null
    var pageNumber = 0
    var y = 0f
    var inLedger = false
    val width = 523
    val bottom = 790f
    val ink = Color.rgb(31, 45, 54)
    val accent = Color.rgb(11, 122, 83)
    fun text(bn: String, en: String) = if (bangla) bn else en
    val locale = if (bangla) Locale("bn", "BD") else Locale.US
    val dates = SimpleDateFormat("dd MMM yyyy", locale)
    fun date(time: Long) = dates.format(Date(time))
    fun number(value: BigDecimal) = String.format(locale, "%,.2f", value.setScale(2, RoundingMode.HALF_UP))
    fun balance(value: BigDecimal): String {
        val displayed = value.setScale(2, RoundingMode.HALF_UP)
        return when (displayed.signum()) {
            1 -> "$currency ${number(displayed)} " + text("পাবো", "Receivable")
            -1 -> "$currency ${number(displayed.abs())} " + text("দেবো", "Payable")
            else -> "$currency ${number(BigDecimal.ZERO)} " + text("সমান", "Settled")
        }
    }
    fun layout(value: String, cellWidth: Int, bold: Boolean = false, size: Float = 11f): StaticLayout {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ink
            textSize = size
            typeface = if (bold) Typeface.create("sans-serif", Typeface.BOLD) else Typeface.create("sans-serif", Typeface.NORMAL)
        }
        return StaticLayout.Builder.obtain(value, 0, value.length, paint, cellWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setIncludePad(true)
            .setLineSpacing(2f, 1f)
            .build()
    }
    fun draw(value: StaticLayout, x: Float, top: Float) {
        val canvas = requireNotNull(activePage).canvas
        canvas.save()
        canvas.translate(x, top)
        value.draw(canvas)
        canvas.restore()
    }
    val widths = listOf(72, 215, 100, 136)
    val headers = listOf(text("তারিখ", "Date"), text("লেনদেন / বিবরণ", "Action / Details"), text("পরিমাণ", "Amount"), text("চলতি হিসাব", "Balance"))
    fun tableHeader() {
        val cells = headers.mapIndexed { i, title -> layout(title, widths[i] - 12, true) }
        val height = cells.maxOf { it.height } + 14f
        requireNotNull(activePage).canvas.drawRect(36f, y, 559f, y + height, Paint().apply { color = Color.rgb(232, 243, 237) })
        var x = 42f
        cells.forEachIndexed { i, cell -> draw(cell, x, y + 7); x += widths[i] }
        y += height
    }
    fun finishPage() {
        activePage?.let { page ->
            draw(layout("Hisabi Khata  •  " + text("পৃষ্ঠা $pageNumber", "Page $pageNumber"), width, size = 9f), 36f, 808f)
            document.finishPage(page)
            activePage = null
        }
    }
    fun nextPage() {
        finishPage()
        pageNumber++
        activePage = document.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
        val canvas = requireNotNull(activePage).canvas
        canvas.drawColor(Color.WHITE)
        canvas.drawRect(36f, 28f, 559f, 32f, Paint().apply { color = accent })
        draw(layout("Hisabi Khata  |  " + text("হিসাব বিবরণী", "Ledger statement"), width, true, 16f), 36f, 42f)
        y = 80f
        if (inLedger) tableHeader()
    }
    // Split tall rows at text-line boundaries, so long Bengali notes cannot overlap or disappear.
    fun row(values: List<String>, columnWidths: List<Int> = listOf(width), bold: Boolean = false) {
        val cells = values.mapIndexed { i, value -> layout(value, columnWidths[i] - 12, bold) }
        val firstLines = IntArray(cells.size)
        do {
            if (bottom - y < 45f) nextPage()
            val available = (bottom - y - 14).toInt()
            val lastLines = IntArray(cells.size)
            var height = 0
            cells.forEachIndexed { i, cell ->
                val start = firstLines[i]
                var end = start
                while (end < cell.lineCount && cell.getLineBottom(end) - cell.getLineTop(start) <= available) end++
                lastLines[i] = end
                if (end > start) height = maxOf(height, cell.getLineBottom(end - 1) - cell.getLineTop(start))
            }
            check(height > 0) { "Unable to lay out statement row" }
            var x = 42f
            val canvas = requireNotNull(activePage).canvas
            cells.forEachIndexed { i, cell ->
                if (lastLines[i] > firstLines[i]) {
                    val top = cell.getLineTop(firstLines[i])
                    val sliceHeight = cell.getLineBottom(lastLines[i] - 1) - top
                    canvas.save()
                    canvas.clipRect(x, y + 7, x + columnWidths[i] - 12, y + 7 + sliceHeight)
                    canvas.translate(x, y + 7 - top)
                    cell.draw(canvas)
                    canvas.restore()
                }
                firstLines[i] = lastLines[i]
                x += columnWidths[i]
            }
            y += height + 14f
            canvas.drawLine(36f, y, 559f, y, Paint().apply { color = Color.LTGRAY; strokeWidth = 0.4f })
            if (cells.indices.any { firstLines[it] < cells[it].lineCount }) nextPage()
        } while (cells.indices.any { firstLines[it] < cells[it].lineCount })
    }
    try {
        currentCoroutineContext().ensureActive()
        nextPage()
        val workspace = when (statement.person.workspace) {
            "PERSONAL" -> text("ব্যক্তিগত", "Personal")
            "SHOP" -> text("ব্যবসা", "Business")
            else -> text("পরিবার", "Family")
        }
        row(listOf(statement.person.name), bold = true)
        if (statement.person.phone.isNotBlank()) row(listOf(statement.person.phone))
        row(listOf("$workspace  •  ${date(statement.startInclusive)} — ${date(statement.endExclusive - 1)}"))
        row(listOf(text("শুরুর হিসাব: ", "Opening balance: ") + balance(statement.openingBalance)), bold = true)
        row(listOf(text("পাবো/দেবো অ্যাপ ব্যবহারকারীর দৃষ্টিকোণ থেকে দেখানো হয়েছে।", "Receivable/payable balances are from the app owner's perspective.")))
        inLedger = true
        if (bottom - y < 90) nextPage() else tableHeader()
        if (statement.rows.isEmpty()) row(listOf(text("এই সময়সীমায় কোনো লেনদেন নেই।", "No transactions in this period.")))
        statement.rows.forEach { item ->
            currentCoroutineContext().ensureActive()
            val entry = item.entry
            val action = when (entry.action) {
                "GAVE" -> text("দিলাম", "Gave")
                "RECEIVED_BACK" -> text("ফেরত পেলাম", "Received back")
                "TOOK" -> text("নিলাম", "Took")
                "PAID_BACK" -> text("ফেরত দিলাম", "Paid back")
                else -> entry.action
            }
            val details = buildString {
                append(action)
                if (entry.note.isNotBlank()) append("\n" + entry.note)
                entry.dueAt?.let { append("\n" + text("পরিশোধের তারিখ: ", "Due: ") + date(it)) }
            }
            row(listOf(date(entry.createdAt), details, "$currency ${number(BigDecimal.valueOf(entry.amount))}", balance(item.balance)), widths)
        }
        inLedger = false
        row(listOf(text("শেষের হিসাব: ", "Closing balance: ") + balance(statement.closingBalance)), bold = true)
        row(listOf(text("লেনদেন: ${statement.rows.size} টি", "Transactions: ${statement.rows.size}") + "  •  " + text("তৈরি: ", "Generated: ") + date(System.currentTimeMillis())))
        finishPage()
        file.outputStream().use { document.writeTo(it) }
        return StatementPdf(file, pageNumber)
    } catch (error: Throwable) {
        file.delete()
        throw error
    } finally {
        activePage?.let { document.finishPage(it) }
        document.close()
    }
}
