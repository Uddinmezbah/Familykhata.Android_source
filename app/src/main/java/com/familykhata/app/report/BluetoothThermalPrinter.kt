package com.familykhata.app.report

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.familykhata.app.data.RetailSaleEntity
import com.familykhata.app.data.RetailSaleLineEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.ceil
import kotlin.math.min

data class ThermalPrinterDevice(
    val name: String,
    val address: String
)

private data class ThermalReceiptRow(
    val text: String,
    val sizePx: Float,
    val bold: Boolean = false,
    val align: Int = 0,
    val gapAfterPx: Int = 4
)

private val THERMAL_SPP_UUID:
    UUID =
    UUID.fromString(
        "00001101-0000-1000-8000-00805F9B34FB"
    )

@SuppressLint("MissingPermission")
fun pairedThermalPrinters(
    context: Context
): List<ThermalPrinterDevice> {
    val manager =
        context.getSystemService(
            BluetoothManager::class.java
        ) ?: return emptyList()

    val adapter =
        manager.adapter
            ?: return emptyList()

    if (!adapter.isEnabled) {
        return emptyList()
    }

    return adapter.bondedDevices
        .map { device ->
            ThermalPrinterDevice(
                name =
                    device.name
                        ?.trim()
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: device.address,
                address =
                    device.address
            )
        }
        .sortedBy {
            it.name.lowercase()
        }
}

private fun thermalMoney(
    value: Double
): String =
    String.format(
        Locale.US,
        "%.2f",
        value
    )

private fun thermalDate(
    time: Long
): String =
    SimpleDateFormat(
        "dd-MM-yyyy HH:mm",
        Locale.US
    ).format(
        Date(time)
    )

private fun configuredPaint(
    row: ThermalReceiptRow
): Paint =
    Paint(
        Paint.ANTI_ALIAS_FLAG
    ).apply {
        color =
            Color.BLACK

        textSize =
            row.sizePx

        typeface =
            Typeface.create(
                Typeface.DEFAULT,
                if (row.bold) {
                    Typeface.BOLD
                } else {
                    Typeface.NORMAL
                }
            )

        textAlign =
            when (row.align) {
                1 ->
                    Paint.Align.CENTER

                2 ->
                    Paint.Align.RIGHT

                else ->
                    Paint.Align.LEFT
            }
    }

private fun wrapThermalText(
    text: String,
    paint: Paint,
    maxWidth: Float
): List<String> {
    if (text.isBlank()) {
        return listOf("")
    }

    val result =
        mutableListOf<String>()

    var remaining =
        text.trim()

    while (remaining.isNotEmpty()) {
        var count =
            paint.breakText(
                remaining,
                true,
                maxWidth,
                null
            )

        if (count <= 0) {
            count = 1
        }

        if (count >= remaining.length) {
            result += remaining
            break
        }

        val prefix =
            remaining.substring(
                0,
                count
            )

        val spaceIndex =
            prefix.lastIndexOf(' ')

        val splitAt =
            if (spaceIndex > 0) {
                spaceIndex
            } else {
                count
            }

        result +=
            remaining
                .substring(
                    0,
                    splitAt
                )
                .trimEnd()

        remaining =
            remaining
                .substring(
                    splitAt
                )
                .trimStart()
    }

    return result
}

private fun receiptRows(
    sale: RetailSaleEntity,
    lines: List<RetailSaleLineEntity>,
    currency: String,
    paperWidthMm: Int,
    businessName: String,
    businessPhone: String,
    businessAddress: String
): List<ThermalReceiptRow> {
    val normal =
        if (paperWidthMm == 80) {
            25f
        } else {
            21f
        }

    val small =
        if (paperWidthMm == 80) {
            21f
        } else {
            18f
        }

    val header =
        if (paperWidthMm == 80) {
            34f
        } else {
            29f
        }

    val separator =
        if (paperWidthMm == 80) {
            "--------------------------------------------"
        } else {
            "------------------------------"
        }

    val due =
        (
            sale.total -
                sale.paid
            ).coerceAtLeast(
                0.0
            )

    val receiptBusinessName =
        businessName
            .trim()
            .ifBlank {
                "হিসাবী খাতা"
            }

    return buildList {
        add(
            ThermalReceiptRow(
                text =
                    receiptBusinessName,
                sizePx =
                    header,
                bold =
                    true,
                align =
                    1,
                gapAfterPx =
                    3
            )
        )

        if (
            businessPhone
                .trim()
                .isNotBlank()
        ) {
            add(
                ThermalReceiptRow(
                    text =
                        businessPhone.trim(),
                    sizePx =
                        small,
                    align =
                        1,
                    gapAfterPx =
                        2
                )
            )
        }

        if (
            businessAddress
                .trim()
                .isNotBlank()
        ) {
            add(
                ThermalReceiptRow(
                    text =
                        businessAddress.trim(),
                    sizePx =
                        small,
                    align =
                        1,
                    gapAfterPx =
                        4
                )
            )
        }

        add(
            ThermalReceiptRow(
                text =
                    "Powered by Hisabi Khata",
                sizePx =
                    small,
                align =
                    1,
                gapAfterPx =
                    8
            )
        )

        add(
            ThermalReceiptRow(
                separator,
                small,
                align = 1
            )
        )

        add(
            ThermalReceiptRow(
                "Invoice: ${sale.invoiceNo}",
                normal,
                bold = true
            )
        )

        add(
            ThermalReceiptRow(
                "Date: ${thermalDate(sale.soldAt)}",
                small
            )
        )

        if (
            sale.customerName
                .isNotBlank()
        ) {
            add(
                ThermalReceiptRow(
                    "Customer: ${sale.customerName}",
                    small
                )
            )
        }

        if (
            sale.customerPhone
                .isNotBlank()
        ) {
            add(
                ThermalReceiptRow(
                    "Phone: ${sale.customerPhone}",
                    small
                )
            )
        }

        add(
            ThermalReceiptRow(
                separator,
                small,
                align = 1
            )
        )

        add(
            ThermalReceiptRow(
                "ITEMS",
                normal,
                bold = true
            )
        )

        lines.forEachIndexed {
                index,
                line ->

            add(
                ThermalReceiptRow(
                    "${index + 1}. ${line.productNameSnapshot}",
                    normal,
                    bold = true,
                    gapAfterPx = 1
                )
            )

            if (
                line.skuSnapshot
                    .isNotBlank()
            ) {
                add(
                    ThermalReceiptRow(
                        "SKU: ${line.skuSnapshot}",
                        small,
                        gapAfterPx = 1
                    )
                )
            }

            add(
                ThermalReceiptRow(
                    "${line.quantity} ${line.unitSnapshot} x " +
                        "$currency${thermalMoney(line.unitPrice)} = " +
                        "$currency${thermalMoney(line.lineTotal)}",
                    small,
                    gapAfterPx = 7
                )
            )
        }

        add(
            ThermalReceiptRow(
                separator,
                small,
                align = 1
            )
        )

        add(
            ThermalReceiptRow(
                "Subtotal: $currency${thermalMoney(sale.subtotal)}",
                normal,
                align = 2
            )
        )

        if (
            sale.discount >
            0.0001
        ) {
            add(
                ThermalReceiptRow(
                    "Discount: $currency${thermalMoney(sale.discount)}",
                    normal,
                    align = 2
                )
            )
        }

        add(
            ThermalReceiptRow(
                "TOTAL: $currency${thermalMoney(sale.total)}",
                normal,
                bold = true,
                align = 2
            )
        )

        add(
            ThermalReceiptRow(
                "Paid: $currency${thermalMoney(sale.paid)}",
                normal,
                align = 2
            )
        )

        add(
            ThermalReceiptRow(
                "Due: $currency${thermalMoney(due)}",
                normal,
                bold =
                    due > 0.0001,
                align = 2
            )
        )

        add(
            ThermalReceiptRow(
                "Payment: ${sale.paymentMethod}",
                small
            )
        )

        add(
            ThermalReceiptRow(
                "Status: ${sale.status}",
                small
            )
        )

        if (
            sale.note.isNotBlank()
        ) {
            add(
                ThermalReceiptRow(
                    "Note: ${sale.note}",
                    small
                )
            )
        }

        add(
            ThermalReceiptRow(
                separator,
                small,
                align = 1
            )
        )

        add(
            ThermalReceiptRow(
                "ধন্যবাদ",
                normal,
                bold = true,
                align = 1,
                gapAfterPx = 2
            )
        )

        add(
            ThermalReceiptRow(
                "Thank you",
                small,
                align = 1,
                gapAfterPx = 12
            )
        )
    }
}

private fun renderThermalReceipt(
    sale: RetailSaleEntity,
    lines: List<RetailSaleLineEntity>,
    currency: String,
    paperWidthMm: Int,
    businessName: String,
    businessPhone: String,
    businessAddress: String
): Bitmap {
    val widthPx =
        if (paperWidthMm == 80) {
            576
        } else {
            384
        }

    val margin =
        if (paperWidthMm == 80) {
            22
        } else {
            14
        }

    val maxTextWidth =
        (
            widthPx -
                margin * 2
            ).toFloat()

    val rows =
        receiptRows(
            sale = sale,
            lines = lines,
            currency = currency,
            paperWidthMm = paperWidthMm,
            businessName =
                businessName,
            businessPhone =
                businessPhone,
            businessAddress =
                businessAddress
        )

    var height =
        margin

    rows.forEach { row ->
        val paint =
            configuredPaint(row)

        val wrapped =
            wrapThermalText(
                row.text,
                paint,
                maxTextWidth
            )

        val metrics =
            paint.fontMetrics

        val lineHeight =
            ceil(
                metrics.descent -
                    metrics.ascent
            ).toInt()

        height +=
            wrapped.size *
                (
                    lineHeight +
                        row.gapAfterPx
                    )
    }

    height +=
        margin +
            48

    val bitmap =
        Bitmap.createBitmap(
            widthPx,
            height.coerceAtLeast(
                120
            ),
            Bitmap.Config.ARGB_8888
        )

    val canvas =
        Canvas(bitmap)

    canvas.drawColor(
        Color.WHITE
    )

    var y =
        margin.toFloat()

    rows.forEach { row ->
        val paint =
            configuredPaint(row)

        val wrapped =
            wrapThermalText(
                row.text,
                paint,
                maxTextWidth
            )

        val metrics =
            paint.fontMetrics

        wrapped.forEach { line ->
            y +=
                -metrics.ascent

            val x =
                when (row.align) {
                    1 ->
                        widthPx /
                            2f

                    2 ->
                        (
                            widthPx -
                                margin
                            ).toFloat()

                    else ->
                        margin.toFloat()
                }

            canvas.drawText(
                line,
                x,
                y,
                paint
            )

            y +=
                metrics.descent +
                    row.gapAfterPx
        }
    }

    return bitmap
}

private fun writeEscPosRaster(
    output: OutputStream,
    bitmap: Bitmap
) {
    val width =
        bitmap.width

    val bytesPerRow =
        (
            width +
                7
            ) /
            8

    val chunkHeight =
        192

    var top =
        0

    while (top < bitmap.height) {
        val height =
            min(
                chunkHeight,
                bitmap.height -
                    top
            )

        val pixels =
            IntArray(
                width *
                    height
            )

        bitmap.getPixels(
            pixels,
            0,
            width,
            0,
            top,
            width,
            height
        )

        val raster =
            ByteArray(
                bytesPerRow *
                    height
            )

        for (
            y in
            0 until height
        ) {
            for (
                x in
                0 until width
            ) {
                val pixel =
                    pixels[
                        y * width +
                            x
                    ]

                val luminance =
                    (
                        Color.red(pixel) *
                            299 +
                            Color.green(pixel) *
                            587 +
                            Color.blue(pixel) *
                            114
                        ) /
                        1000

                if (
                    Color.alpha(pixel) >
                    0 &&
                    luminance <
                    180
                ) {
                    val byteIndex =
                        y *
                            bytesPerRow +
                            x /
                            8

                    val bit =
                        7 -
                            (
                                x %
                                    8
                                )

                    raster[byteIndex] =
                        (
                            raster[byteIndex]
                                .toInt() or
                                (
                                    1 shl
                                        bit
                                    )
                            ).toByte()
                }
            }
        }

        output.write(
            byteArrayOf(
                0x1D,
                0x76,
                0x30,
                0x00,
                (
                    bytesPerRow and
                        0xFF
                    ).toByte(),
                (
                    bytesPerRow shr
                        8 and
                        0xFF
                    ).toByte(),
                (
                    height and
                        0xFF
                    ).toByte(),
                (
                    height shr
                        8 and
                        0xFF
                    ).toByte()
            )
        )

        output.write(
            raster
        )

        top +=
            height
    }
}

@SuppressLint("MissingPermission")
private suspend fun withThermalOutput(
    context: Context,
    address: String,
    block: (OutputStream) -> Unit
) {
    withContext(Dispatchers.IO) {
        val manager =
            requireNotNull(
                context.getSystemService(
                    BluetoothManager::class.java
                )
            ) {
                "Bluetooth unavailable"
            }

        val adapter =
            requireNotNull(
                manager.adapter
            ) {
                "Bluetooth unavailable"
            }

        require(
            adapter.isEnabled
        ) {
            "Bluetooth is turned off"
        }

        val device =
            adapter.getRemoteDevice(
                address
            )

        var socket =
            device.createRfcommSocketToServiceRecord(
                THERMAL_SPP_UUID
            )

        try {
            try {
                socket.connect()
            } catch (
                secureError:
                    Exception
            ) {
                runCatching {
                    socket.close()
                }

                socket =
                    device
                        .createInsecureRfcommSocketToServiceRecord(
                            THERMAL_SPP_UUID
                        )

                try {
                    socket.connect()
                } catch (
                    insecureError:
                        Exception
                ) {
                    insecureError.addSuppressed(
                        secureError
                    )

                    throw insecureError
                }
            }

            val output =
                socket.outputStream

            output.write(
                byteArrayOf(
                    0x1B,
                    0x40
                )
            )

            block(output)

            output.flush()
        } finally {
            runCatching {
                socket.close()
            }
        }
    }
}

@SuppressLint("MissingPermission")
suspend fun printThermalTest(
    context: Context,
    address: String,
    paperWidthMm: Int
) {
    require(
        paperWidthMm == 58 ||
            paperWidthMm == 80
    ) {
        "Unsupported paper width"
    }

    withThermalOutput(
        context = context,
        address = address
    ) { output ->
        output.write(
            byteArrayOf(
                0x1B,
                0x61,
                0x01
            )
        )

        output.write(
            byteArrayOf(
                0x1B,
                0x45,
                0x01
            )
        )

        output.write(
            "HISABI KHATA\n"
                .toByteArray(
                    Charsets.US_ASCII
                )
        )

        output.write(
            byteArrayOf(
                0x1B,
                0x45,
                0x00
            )
        )

        output.write(
            byteArrayOf(
                0x1B,
                0x61,
                0x00
            )
        )

        val columns =
            if (paperWidthMm == 80) {
                48
            } else {
                32
            }

        val separator =
            "-".repeat(
                columns
            )

        val receipt =
            buildString {
                appendLine(separator)
                appendLine(
                    "Bluetooth Thermal Printer"
                )
                appendLine(
                    "Paper: ${paperWidthMm}mm"
                )
                appendLine(separator)
                appendLine(
                    "Connection: OK"
                )
                appendLine(
                    "ESC/POS test: OK"
                )
                appendLine(separator)
                appendLine()
                appendLine()
                appendLine()
            }

        output.write(
            receipt.toByteArray(
                Charsets.US_ASCII
            )
        )
    }
}

@SuppressLint("MissingPermission")
suspend fun printThermalInvoice(
    context: Context,
    address: String,
    paperWidthMm: Int,
    sale: RetailSaleEntity,
    lines: List<RetailSaleLineEntity>,
    currency: String,
    businessName: String = "",
    businessPhone: String = "",
    businessAddress: String = ""
) {
    require(
        paperWidthMm == 58 ||
            paperWidthMm == 80
    ) {
        "Unsupported paper width"
    }

    require(
        lines.isNotEmpty()
    ) {
        "Invoice has no sale lines"
    }

    val bitmap =
        withContext(
            Dispatchers.Default
        ) {
            renderThermalReceipt(
                sale = sale,
                lines = lines,
                currency = currency,
                paperWidthMm =
                    paperWidthMm,
                businessName =
                    businessName,
                businessPhone =
                    businessPhone,
                businessAddress =
                    businessAddress
            )
        }

    try {
        withThermalOutput(
            context = context,
            address = address
        ) { output ->
            output.write(
                byteArrayOf(
                    0x1B,
                    0x61,
                    0x00
                )
            )

            writeEscPosRaster(
                output,
                bitmap
            )

            output.write(
                byteArrayOf(
                    0x0A,
                    0x0A,
                    0x0A
                )
            )
        }
    } finally {
        bitmap.recycle()
    }
}