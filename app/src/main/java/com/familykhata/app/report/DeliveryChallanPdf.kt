package com.familykhata.app.report

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.familykhata.app.dealerbusiness.DealerDeliveryChallanEntity
import com.familykhata.app.dealerbusiness.DealerDeliveryChallanLineEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

data class DeliveryChallanPdf(
    val file: File,
    val pageCount: Int
)

suspend fun writeDeliveryChallanPdf(
    context: Context,
    challan: DealerDeliveryChallanEntity,
    lines: List<DealerDeliveryChallanLineEntity>,
    bangla: Boolean
): DeliveryChallanPdf {
    require(lines.isNotEmpty()) {
        "Delivery challan has no lines"
    }

    val directory =
        File(
            context.cacheDir,
            "delivery_challans"
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
            "HisabiKhata-challan-${challan.id}-",
            ".pdf",
            directory
        )

    val document =
        PdfDocument()

    var activePage:
        PdfDocument.Page? = null

    var pageNumber = 0
    var y = 0f
    var inItems = false

    val width = 523
    val bottom = 790f

    val ink =
        Color.rgb(
            31,
            45,
            54
        )

    val accent =
        Color.rgb(
            11,
            122,
            83
        )

    val lightAccent =
        Color.rgb(
            232,
            243,
            237
        )

    fun text(
        bn: String,
        en: String
    ): String =
        if (bangla) {
            bn
        } else {
            en
        }

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
            "dd MMM yyyy, hh:mm a",
            locale
        )

    fun date(
        value: Long
    ): String =
        dateFormat.format(
            Date(value)
        )

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
                    if (bold) {
                        Typeface.create(
                            "sans-serif",
                            Typeface.BOLD
                        )
                    } else {
                        Typeface.create(
                            "sans-serif",
                            Typeface.NORMAL
                        )
                    }
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
                activePage
            ).canvas

        canvas.save()

        canvas.translate(
            x,
            top
        )

        value.draw(
            canvas
        )

        canvas.restore()
    }

    val itemWidths =
        listOf(
            275,
            120,
            128
        )

    val itemHeaders =
        listOf(
            text(
                "পণ্য",
                "Item"
            ),
            text(
                "দেওয়া হয়েছে",
                "Issued"
            ),
            text(
                "বেস পরিমাণ",
                "Base qty"
            )
        )

    fun tableHeader() {
        val cells =
            itemHeaders.mapIndexed {
                    index,
                    title ->

                layout(
                    title,
                    itemWidths[index] - 12,
                    bold = true
                )
            }

        val height =
            cells.maxOf {
                it.height
            } + 14f

        requireNotNull(
            activePage
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
                itemWidths[index]
        }

        y +=
            height
    }

    fun finishPage() {
        activePage?.let {
                page ->

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
                page
            )

            activePage =
                null
        }
    }

    fun nextPage() {
        finishPage()

        pageNumber++

        activePage =
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
                activePage
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
                        "ডেলিভারি চালান",
                        "Delivery Challan"
                    ),
                width,
                bold = true,
                size = 16f
            ),
            36f,
            42f
        )

        y = 80f

        if (inItems) {
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
                    cell.getLineBottom(
                        end
                    ) -
                        cell.getLineTop(
                            start
                        ) <=
                    available
                ) {
                    end++
                }

                lastLines[index] =
                    end

                if (
                    end >
                    start
                ) {
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
            ) {
                "Unable to lay out challan row"
            }

            var x = 42f

            val canvas =
                requireNotNull(
                    activePage
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
                        ) -
                            top

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

    fun statusLabel(): String =
        when (
            challan.status
                .uppercase(
                    Locale.ROOT
                )
        ) {
            "OPEN" ->
                text(
                    "খোলা",
                    "Open"
                )

            "SETTLED" ->
                text(
                    "বুঝে নেওয়া হয়েছে",
                    "Settled"
                )

            else ->
                challan.status
        }

    try {
        currentCoroutineContext()
            .ensureActive()

        nextPage()

        row(
            listOf(
                text(
                    "চালান নম্বর: ",
                    "Challan no: "
                ) +
                    challan.challanNo
            ),
            bold = true
        )

        row(
            listOf(
                text(
                    "তারিখ: ",
                    "Date: "
                ) +
                    date(
                        challan.issuedAt
                    )
            )
        )

        row(
            listOf(
                text(
                    "ডেলিভারি ম্যান: ",
                    "Delivery person: "
                ) +
                    challan
                        .deliveryPersonNameSnapshot
            )
        )

        row(
            listOf(
                text(
                    "অবস্থা: ",
                    "Status: "
                ) +
                    statusLabel()
            )
        )

        challan.settledAt
            ?.let {
                row(
                    listOf(
                        text(
                            "বুঝে নেওয়ার সময়: ",
                            "Settled at: "
                        ) +
                            date(it)
                    )
                )
            }

        row(
            listOf(
                text(
                    "এটি পণ্য হস্তান্তরের চালান; পেমেন্ট রসিদ নয়।",
                    "This is a goods handover challan, not a payment receipt."
                )
            ),
            bold = true
        )

        inItems =
            true

        if (
            bottom - y <
            90f
        ) {
            nextPage()
        } else {
            tableHeader()
        }

        lines.forEach {
                line ->

            currentCoroutineContext()
                .ensureActive()

            val unitName =
                line.unitSnapshot
                    .trim()
                    .ifBlank {
                        text(
                            "ইউনিট",
                            "unit"
                        )
                    }

            val entered =
                if (
                    line.enteredQuantity >
                    0
                ) {
                    line.enteredQuantity
                } else {
                    line.quantityPieces
                }

            val baseLabel =
                text(
                    "বেস ইউনিট",
                    "base units"
                )

            val baseDisplay =
                if (
                    line.unitFactor >
                    1
                ) {
                    "${line.quantityPieces} $baseLabel\n" +
                        "1 $unitName = " +
                        "${line.unitFactor} $baseLabel"
                } else {
                    "${line.quantityPieces} $unitName"
                }

            row(
                listOf(
                    line.productNameSnapshot,
                    "$entered $unitName",
                    baseDisplay
                ),
                itemWidths
            )
        }

        inItems =
            false

        if (
            challan.note
                .isNotBlank()
        ) {
            row(
                listOf(
                    text(
                        "নোট: ",
                        "Note: "
                    ) +
                        challan.note
                )
            )
        }

        row(
            listOf(
                text(
                    "Hisabi Khata দ্বারা তৈরি",
                    "Generated by Hisabi Khata"
                )
            )
        )

        finishPage()

        file.outputStream()
            .use {
                document.writeTo(
                    it
                )
            }

        return DeliveryChallanPdf(
            file =
                file,
            pageCount =
                pageNumber
        )
    } catch (
        error: Throwable
    ) {
        file.delete()
        throw error
    } finally {
        activePage?.let {
            document.finishPage(
                it
            )
        }

        document.close()
    }
}
