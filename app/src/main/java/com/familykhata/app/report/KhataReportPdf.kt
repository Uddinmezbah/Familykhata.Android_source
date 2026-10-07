package com.familykhata.app.report

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PageRange
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.content.Context
import com.familykhata.app.data.TransactionEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

data class KhataReportPdf(
    val file: File,
    val pageCount: Int
)

suspend fun writeKhataReportPdf(
    context: Context,
    khataName: String,
    workspace: String,
    transactions: List<TransactionEntity>,
    bangla: Boolean,
    currency: String
): KhataReportPdf {
    val directory =
        File(
            context.cacheDir,
            "khata_reports"
        ).apply {
            mkdirs()
        }

    val oldest =
        System.currentTimeMillis() -
            7L * 24 * 60 * 60 * 1000

    directory.listFiles()
        ?.filter {
            it.isFile &&
                it.lastModified() < oldest
        }
        ?.forEach {
            it.delete()
        }

    val file =
        File.createTempFile(
            "HisabiKhata-khata-report-",
            ".pdf",
            directory
        )

    val income =
        transactions
            .filter {
                it.type == "INCOME"
            }
            .sumOf {
                it.amount
            }

    val expense =
        transactions
            .filter {
                it.type == "EXPENSE"
            }
            .sumOf {
                it.amount
            }

    val balance =
        income - expense

    val document = PdfDocument()
    var page: PdfDocument.Page? = null
    var pageNumber = 0
    var y = 0f
    var inTable = false

    val width = 523
    val bottom = 790f
    val ink = Color.rgb(31, 45, 54)
    val accent = Color.rgb(20, 121, 184)
    val lightAccent = Color.rgb(232, 243, 250)

    fun text(
        bn: String,
        en: String
    ): String =
        if (bangla) bn else en

    val locale =
        if (bangla) {
            Locale(
                "bn",
                "BD"
            )
        } else {
            Locale.US
        }

    val dateFormat =
        SimpleDateFormat(
            "dd MMM yyyy",
            locale
        )

    val dateTimeFormat =
        SimpleDateFormat(
            "dd MMM yyyy, hh:mm a",
            locale
        )

    fun money(
        value: Double
    ): String =
        "$currency ${
            String.format(
                locale,
                "%,.2f",
                value
            )
        }"

    fun layout(
        value: String,
        cellWidth: Int,
        bold: Boolean = false,
        size: Float = 11f
    ): StaticLayout {
        val paint =
            TextPaint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {
                color = ink
                textSize = size
                typeface =
                    Typeface.create(
                        "sans-serif",
                        if (bold) {
                            Typeface.BOLD
                        } else {
                            Typeface.NORMAL
                        }
                    )
            }

        return StaticLayout.Builder
            .obtain(
                value,
                0,
                value.length,
                paint,
                cellWidth
            )
            .setAlignment(
                Layout.Alignment.ALIGN_NORMAL
            )
            .setIncludePad(true)
            .setLineSpacing(
                2f,
                1f
            )
            .build()
    }

    fun draw(
        value: StaticLayout,
        x: Float,
        top: Float
    ) {
        val canvas =
            requireNotNull(
                page
            ).canvas

        canvas.save()
        canvas.translate(
            x,
            top
        )
        value.draw(canvas)
        canvas.restore()
    }

    val widths =
        listOf(
            78,
            74,
            235,
            136
        )

    val headers =
        listOf(
            text(
                "তারিখ",
                "Date"
            ),
            text(
                "ধরন",
                "Type"
            ),
            text(
                "বিভাগ / বিবরণ",
                "Category / Details"
            ),
            text(
                "পরিমাণ",
                "Amount"
            )
        )

    fun tableHeader() {
        val cells =
            headers.mapIndexed {
                    index,
                    title ->
                layout(
                    title,
                    widths[index] - 12,
                    bold = true
                )
            }

        val height =
            cells.maxOf {
                it.height
            } + 14f

        requireNotNull(
            page
        ).canvas.drawRect(
            36f,
            y,
            559f,
            y + height,
            Paint().apply {
                color =
                    lightAccent
            }
        )

        var x = 42f

        cells.forEachIndexed {
                index,
                cell ->
            draw(
                cell,
                x,
                y + 7
            )
            x +=
                widths[index]
        }

        y += height
    }

    fun finishPage() {
        page?.let {
            draw(
                layout(
                    "Hisabi Khata  •  " +
                        text(
                            "পৃষ্ঠা $pageNumber",
                            "Page $pageNumber"
                        ),
                    width,
                    size = 9f
                ),
                36f,
                808f
            )

            document.finishPage(
                it
            )

            page = null
        }
    }

    fun nextPage() {
        finishPage()

        pageNumber++

        page =
            document.startPage(
                PdfDocument.PageInfo
                    .Builder(
                        595,
                        842,
                        pageNumber
                    )
                    .create()
            )

        val canvas =
            requireNotNull(
                page
            ).canvas

        canvas.drawColor(
            Color.WHITE
        )

        canvas.drawRect(
            36f,
            28f,
            559f,
            32f,
            Paint().apply {
                color =
                    accent
            }
        )

        draw(
            layout(
                "Hisabi Khata  |  " +
                    text(
                        "খাতা রিপোর্ট",
                        "Khata Report"
                    ),
                width,
                bold = true,
                size = 16f
            ),
            36f,
            42f
        )

        y = 80f

        if (inTable) {
            tableHeader()
        }
    }

    fun row(
        values: List<String>,
        columnWidths: List<Int> =
            listOf(width),
        bold: Boolean = false
    ) {
        val cells =
            values.mapIndexed {
                    index,
                    value ->
                layout(
                    value,
                    columnWidths[index] - 12,
                    bold
                )
            }

        val firstLines =
            IntArray(
                cells.size
            )

        do {
            if (
                bottom - y <
                45f
            ) {
                nextPage()
            }

            val available =
                (
                    bottom -
                        y -
                        14
                    ).toInt()

            val lastLines =
                IntArray(
                    cells.size
                )

            var height = 0

            cells.forEachIndexed {
                    index,
                    cell ->
                val start =
                    firstLines[index]

                var end =
                    start

                while (
                    end <
                    cell.lineCount &&
                    cell.getLineBottom(end) -
                        cell.getLineTop(start) <=
                    available
                ) {
                    end++
                }

                lastLines[index] =
                    end

                if (end > start) {
                    height =
                        maxOf(
                            height,
                            cell.getLineBottom(
                                end - 1
                            ) -
                                cell.getLineTop(
                                    start
                                )
                        )
                }
            }

            check(
                height > 0
            )

            var x = 42f
            val canvas =
                requireNotNull(
                    page
                ).canvas

            cells.forEachIndexed {
                    index,
                    cell ->
                if (
                    lastLines[index] >
                    firstLines[index]
                ) {
                    val top =
                        cell.getLineTop(
                            firstLines[index]
                        )

                    val sliceHeight =
                        cell.getLineBottom(
                            lastLines[index] - 1
                        ) - top

                    canvas.save()
                    canvas.clipRect(
                        x,
                        y + 7,
                        x +
                            columnWidths[index] -
                            12,
                        y +
                            7 +
                            sliceHeight
                    )
                    canvas.translate(
                        x,
                        y + 7 - top
                    )
                    cell.draw(
                        canvas
                    )
                    canvas.restore()
                }

                firstLines[index] =
                    lastLines[index]

                x +=
                    columnWidths[index]
            }

            y +=
                height +
                    14f

            canvas.drawLine(
                36f,
                y,
                559f,
                y,
                Paint().apply {
                    color =
                        Color.LTGRAY
                    strokeWidth =
                        0.4f
                }
            )

            if (
                cells.indices.any {
                    firstLines[it] <
                        cells[it]
                            .lineCount
                }
            ) {
                nextPage()
            }
        } while (
            cells.indices.any {
                firstLines[it] <
                    cells[it]
                        .lineCount
            }
        )
    }

    try {
        currentCoroutineContext()
            .ensureActive()

        nextPage()

        val workspaceLabel =
            when (workspace) {
                "PERSONAL" ->
                    text(
                        "ব্যক্তিগত",
                        "Personal"
                    )

                "FAMILY" ->
                    text(
                        "পরিবার",
                        "Family"
                    )

                else ->
                    workspace
            }

        row(
            listOf(
                khataName
            ),
            bold = true
        )

        row(
            listOf(
                workspaceLabel +
                    "  •  " +
                    text(
                        "তৈরি: ",
                        "Generated: "
                    ) +
                    dateTimeFormat.format(
                        Date()
                    )
            )
        )

        row(
            listOf(
                text(
                    "মোট আয়: ",
                    "Total income: "
                ) +
                    money(
                        income
                    )
            ),
            bold = true
        )

        row(
            listOf(
                text(
                    "মোট খরচ: ",
                    "Total expense: "
                ) +
                    money(
                        expense
                    )
            ),
            bold = true
        )

        row(
            listOf(
                text(
                    "বর্তমান ব্যালেন্স: ",
                    "Current balance: "
                ) +
                    money(
                        balance
                    )
            ),
            bold = true
        )

        inTable = true

        if (
            bottom - y <
            90f
        ) {
            nextPage()
        } else {
            tableHeader()
        }

        if (
            transactions.isEmpty()
        ) {
            row(
                listOf(
                    text(
                        "এই খাতায় কোনো আয়-খরচ নেই।",
                        "No income or expense in this khata."
                    )
                )
            )
        }

        transactions
            .sortedWith(
                compareBy<TransactionEntity> {
                    it.createdAt
                }.thenBy {
                    it.id
                }
            )
            .forEach { item ->
                currentCoroutineContext()
                    .ensureActive()

                val typeLabel =
                    if (
                        item.type ==
                            "INCOME"
                    ) {
                        text(
                            "আয়",
                            "Income"
                        )
                    } else {
                        text(
                            "খরচ",
                            "Expense"
                        )
                    }

                val details =
                    buildString {
                        append(
                            item.category
                        )

                        if (
                            item.note
                                .isNotBlank()
                        ) {
                            append("\n")
                            append(
                                item.note
                            )
                        }
                    }

                val amount =
                    if (
                        item.type ==
                            "INCOME"
                    ) {
                        "+${money(item.amount)}"
                    } else {
                        "-${money(item.amount)}"
                    }

                row(
                    listOf(
                        dateFormat.format(
                            Date(
                                item.createdAt
                            )
                        ),
                        typeLabel,
                        details,
                        amount
                    ),
                    widths
                )
            }

        inTable = false

        row(
            listOf(
                text(
                    "লেনদেন: ${transactions.size} টি",
                    "Transactions: ${transactions.size}"
                )
            ),
            bold = true
        )

        finishPage()

        file.outputStream()
            .use {
                document.writeTo(
                    it
                )
            }

        return KhataReportPdf(
            file = file,
            pageCount =
                pageNumber
        )
    } catch (
        error: Throwable
    ) {
        file.delete()
        throw error
    } finally {
        page?.let {
            document.finishPage(
                it
            )
        }
        document.close()
    }
}

class KhataPdfPrintAdapter(
    private val file: File,
    private val documentName: String
) : PrintDocumentAdapter() {
    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes?,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback?,
        extras: Bundle?
    ) {
        if (
            cancellationSignal
                ?.isCanceled ==
                true
        ) {
            callback
                ?.onLayoutCancelled()
            return
        }

        val info =
            PrintDocumentInfo
                .Builder(
                    documentName
                )
                .setContentType(
                    PrintDocumentInfo
                        .CONTENT_TYPE_DOCUMENT
                )
                .setPageCount(
                    PrintDocumentInfo
                        .PAGE_COUNT_UNKNOWN
                )
                .build()

        callback
            ?.onLayoutFinished(
                info,
                true
            )
    }

    override fun onWrite(
        pages: Array<out PageRange>?,
        destination: ParcelFileDescriptor?,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback?
    ) {
        if (
            destination == null ||
            callback == null
        ) {
            return
        }

        if (
            cancellationSignal
                ?.isCanceled ==
                true
        ) {
            callback
                .onWriteCancelled()
            return
        }

        try {
            file.inputStream()
                .use { input ->
                    FileOutputStream(
                        destination.fileDescriptor
                    ).use { output ->
                        input.copyTo(
                            output
                        )
                    }
                }

            callback
                .onWriteFinished(
                    arrayOf(
                        PageRange.ALL_PAGES
                    )
                )
        } catch (
            error: Exception
        ) {
            callback
                .onWriteFailed(
                    error.message
                )
        }
    }
}
