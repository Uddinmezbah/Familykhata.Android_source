package com.familykhata.app.report

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.familykhata.app.data.RetailSaleEntity
import com.familykhata.app.data.RetailSaleLineEntity
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

data class RetailInvoicePdf(
    val file: File,
    val pageCount: Int
)

suspend fun writeRetailInvoicePdf(
    context: Context,
    sale: RetailSaleEntity,
    lines: List<RetailSaleLineEntity>,
    bangla: Boolean,
    currency: String
): RetailInvoicePdf {
    require(lines.isNotEmpty()) {
        "Invoice has no sale lines"
    }

    val directory =
        File(
            context.cacheDir,
            "retail_invoices"
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
            "HisabiKhata-invoice-${sale.id}-",
            ".pdf",
            directory
        )

    val document = PdfDocument()
    var activePage: PdfDocument.Page? = null
    var pageNumber = 0
    var y = 0f
    var inItems = false

    val width = 523
    val bottom = 790f
    val ink = Color.rgb(31, 45, 54)
    val accent = Color.rgb(11, 122, 83)
    val lightAccent = Color.rgb(232, 243, 237)

    val profilePreferences =
        context.getSharedPreferences(
            "hisabi_khata_v14_settings",
            Context.MODE_PRIVATE
        )

    val businessName =
        profilePreferences.getString("business_name", "").orEmpty().trim()

    val businessAddress =
        profilePreferences.getString("business_address", "").orEmpty().trim()

    val businessPhone =
        profilePreferences.getString("profile_phone", "").orEmpty().trim()

    val businessLogoPath =
        profilePreferences.getString("business_logo_path", "").orEmpty().trim()

    val businessLogo =
        runCatching {
            businessLogoPath
                .takeIf { it.isNotBlank() }
                ?.let { path ->
                    File(path)
                        .takeIf { it.isFile }
                        ?.let { file ->
                            android.graphics.BitmapFactory.decodeFile(
                                file.absolutePath
                            )
                        }
                }
        }.getOrNull()

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
            "dd MMM yyyy, hh:mm a",
            locale
        )

    fun date(
        value: Long
    ): String =
        dateFormat.format(
            Date(value)
        )

    fun number(
        value: BigDecimal
    ): String =
        String.format(
            locale,
            "%,.2f",
            value.setScale(
                2,
                RoundingMode.HALF_UP
            )
        )

    fun money(
        value: Double
    ): String =
        "$currency ${
            number(
                BigDecimal.valueOf(value)
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
        value.draw(canvas)
        canvas.restore()
    }

    val itemWidths =
        listOf(
            245,
            65,
            90,
            123
        )

    val itemHeaders =
        listOf(
            text(
                "পণ্য",
                "Item"
            ),
            text(
                "পরিমাণ",
                "Qty"
            ),
            text(
                "দর",
                "Rate"
            ),
            text(
                "মোট",
                "Amount"
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
                color = lightAccent
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

        y += height
    }

    fun finishPage() {
        activePage?.let { page ->
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

            activePage = null
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
                color = accent
            }
        )

        val headerText =
            buildString {
                append(
                    businessName.ifBlank {
                        "Hisabi Khata"
                    }
                )
                append("\n")
                append(
                    text(
                        "বিক্রয় ইনভয়েস",
                        "Sales Invoice"
                    )
                )

                if (businessAddress.isNotBlank()) {
                    append("\n")
                    append(businessAddress)
                }

                if (businessPhone.isNotBlank()) {
                    append("\n")
                    append(
                        text(
                            "ফোন: ",
                            "Phone: "
                        )
                    )
                    append(businessPhone)
                }
            }

        val logoSize =
            if (businessLogo != null) {
                48f
            } else {
                0f
            }

        val headerX =
            if (businessLogo != null) {
                94f
            } else {
                36f
            }

        val headerWidth =
            if (businessLogo != null) {
                465
            } else {
                width
            }

        val headerLayout =
            layout(
                headerText,
                headerWidth,
                bold = true,
                size = 13f
            )

        businessLogo?.let { bitmap ->
            canvas.drawBitmap(
                bitmap,
                null,
                android.graphics.RectF(
                    36f,
                    42f,
                    84f,
                    90f
                ),
                Paint(Paint.ANTI_ALIAS_FLAG)
            )
        }

        draw(
            headerLayout,
            headerX,
            42f
        )

        y =
            maxOf(
                100f,
                42f +
                    maxOf(
                        headerLayout.height.toFloat(),
                        logoSize
                    ) +
                    10f
            )

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
            ) {
                "Unable to lay out invoice row"
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

    fun paymentLabel(): String =
        when (
            sale.paymentMethod.uppercase()
        ) {
            "CASH" ->
                text(
                    "ক্যাশ",
                    "Cash"
                )

            "BKASH" -> "bKash"
            "NAGAD" -> "Nagad"
            "ROCKET" -> "Rocket"

            "BANK" ->
                text(
                    "ব্যাংক",
                    "Bank"
                )

            "CARD" ->
                text(
                    "কার্ড",
                    "Card"
                )

            else ->
                sale.paymentMethod
        }

    fun statusLabel(): String =
        when (sale.status) {
            "PAID" ->
                text(
                    "পরিশোধিত",
                    "Paid"
                )

            "PARTIAL" ->
                text(
                    "আংশিক পরিশোধ",
                    "Partially paid"
                )

            "CANCELLED" ->
                text(
                    "বাতিল",
                    "Cancelled"
                )

            else ->
                text(
                    "বাকি",
                    "Due"
                )
        }

    try {
        currentCoroutineContext()
            .ensureActive()

        nextPage()

        row(
            listOf(
                text(
                    "ইনভয়েস নম্বর: ",
                    "Invoice no: "
                ) +
                    sale.invoiceNo
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
                        sale.soldAt
                    )
            )
        )

        row(
            listOf(
                text(
                    "অবস্থা: ",
                    "Status: "
                ) +
                    statusLabel() +
                    "  •  " +
                    text(
                        "পেমেন্ট: ",
                        "Payment: "
                    ) +
                    paymentLabel()
            )
        )

        if (
            sale.customerName
                .isNotBlank()
        ) {
            row(
                listOf(
                    text(
                        "ক্রেতা: ",
                        "Customer: "
                    ) +
                        sale.customerName
                )
            )
        }

        if (
            sale.customerPhone
                .isNotBlank()
        ) {
            row(
                listOf(
                    text(
                        "মোবাইল: ",
                        "Phone: "
                    ) +
                        sale.customerPhone
                )
            )
        }

        if (
            sale.status ==
            "CANCELLED"
        ) {
            row(
                listOf(
                    text(
                        "এই ইনভয়েসটি বাতিল করা হয়েছে। এটি সক্রিয় বিক্রির হিসাবে গণনা করা হয় না।",
                        "This invoice has been cancelled and is not counted as an active sale."
                    )
                ),
                bold = true
            )
        }

        inItems = true

        if (
            bottom - y <
            90f
        ) {
            nextPage()
        } else {
            tableHeader()
        }

        lines.forEach { line ->
            currentCoroutineContext()
                .ensureActive()

            row(
                listOf(
                    buildString {
                        append(
                            line.productNameSnapshot
                        )

                        if (
                            line.skuSnapshot
                                .isNotBlank()
                        ) {
                            append(
                                "\nSKU: "
                            )

                            append(
                                line.skuSnapshot
                            )
                        }
                    },
                    buildString {
                        append(
                            line.quantity
                        )

                        if (
                            line.unitSnapshot
                                .isNotBlank()
                        ) {
                            append(" ")
                            append(
                                line.unitSnapshot
                            )
                        }
                    },
                    money(
                        line.unitPrice
                    ),
                    money(
                        line.lineTotal
                    )
                ),
                itemWidths
            )
        }

        inItems = false

        val due =
            (
                sale.total -
                    sale.paid
                ).coerceAtLeast(
                    0.0
                )

        row(
            listOf(
                text(
                    "সাবটোটাল: ",
                    "Subtotal: "
                ) +
                    money(
                        sale.subtotal
                    )
            )
        )

        if (
            sale.discount >
            0.0001
        ) {
            row(
                listOf(
                    text(
                        "ছাড়: ",
                        "Discount: "
                    ) +
                        money(
                            sale.discount
                        )
                )
            )
        }

        row(
            listOf(
                text(
                    "মোট: ",
                    "Total: "
                ) +
                    money(
                        sale.total
                    )
            ),
            bold = true
        )

        row(
            listOf(
                text(
                    "আদায়: ",
                    "Paid: "
                ) +
                    money(
                        sale.paid
                    )
            )
        )

        row(
            listOf(
                text(
                    "বাকি: ",
                    "Due: "
                ) +
                    money(
                        due
                    )
            ),
            bold =
                due >
                    0.0001
        )

        if (
            sale.note
                .isNotBlank()
        ) {
            row(
                listOf(
                    text(
                        "নোট: ",
                        "Note: "
                    ) +
                        sale.note
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

        return RetailInvoicePdf(
            file = file,
            pageCount = pageNumber
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
