package com.familykhata.app.report

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.familykhata.app.data.BusinessProfileEntity
import com.familykhata.app.data.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BusinessVoucherPdf(
    val file: File
)

suspend fun writeBusinessVoucherPdf(
    context: Context,
    transaction: TransactionEntity,
    business: BusinessProfileEntity?,
    clientName: String,
    clientPhone: String,
    workDetails: String,
    currencySymbol: String
): BusinessVoucherPdf = withContext(Dispatchers.IO) {
    require(transaction.workspace == "SHOP") {
        "Voucher is available only for business transactions"
    }
    require(transaction.type == "INCOME") {
        "Voucher is available only for business income transactions"
    }
    require(clientName.trim().isNotBlank()) {
        "Client name is required"
    }

    val dir = File(context.cacheDir, "business_vouchers").apply { mkdirs() }
    val file = File.createTempFile("HisabiKhata-voucher-${transaction.id}-", ".pdf", dir)

    val document = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
    val page = document.startPage(pageInfo)
    val canvas = page.canvas

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(37, 57, 82)
        textSize = 24f
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
    }
    val headingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(55, 73, 94)
        textSize = 15f
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
    }
    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(35, 39, 44)
        textSize = 13f
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    }
    val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(105, 111, 119)
        textSize = 11f
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    }
    val amountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(11, 122, 83)
        textSize = 25f
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
    }
    val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(215, 220, 226)
        strokeWidth = 1f
    }

    val margin = 42f
    val contentWidth = 595f - margin * 2f

    fun drawWrapped(
        text: String,
        x: Float,
        yStart: Float,
        paint: Paint,
        maxWidth: Float,
        lineHeight: Float
    ): Float {
        val clean = text.trim().ifBlank { "-" }
        val words = clean.split(Regex("\\s+"))
        var line = ""
        var y = yStart

        fun drawLine(value: String) {
            canvas.drawText(value, x, y, paint)
            y += lineHeight
        }

        for (word in words) {
            val candidate = if (line.isBlank()) word else "$line $word"
            if (paint.measureText(candidate) <= maxWidth) {
                line = candidate
            } else {
                if (line.isNotBlank()) drawLine(line)
                if (paint.measureText(word) <= maxWidth) {
                    line = word
                } else {
                    var chunk = ""
                    word.forEach { ch ->
                        val candidateChunk = chunk + ch
                        if (paint.measureText(candidateChunk) > maxWidth && chunk.isNotBlank()) {
                            drawLine(chunk)
                            chunk = ch.toString()
                        } else {
                            chunk = candidateChunk
                        }
                    }
                    line = chunk
                }
            }
        }

        if (line.isNotBlank()) drawLine(line)
        return y
    }

    val businessName = business?.name?.trim().orEmpty().ifBlank { "Hisabi Khata Business" }
    val businessPhone = business?.phone?.trim().orEmpty()
    val businessAddress = business?.address?.trim().orEmpty()
    val voucherNo = "VCH-" + transaction.id.toString().padStart(6, '0')
    val dateText = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        .format(Date(transaction.createdAt))
    val money = DecimalFormat("#,##0.##").format(transaction.amount)

    var y = 62f
    canvas.drawText(businessName, margin, y, titlePaint)
    y += 24f

    if (businessAddress.isNotBlank()) {
        y = drawWrapped(businessAddress, margin, y, mutedPaint, contentWidth, 15f)
    }
    if (businessPhone.isNotBlank()) {
        canvas.drawText("Phone: $businessPhone", margin, y, mutedPaint)
        y += 18f
    }

    y += 8f
    canvas.drawLine(margin, y, 595f - margin, y, linePaint)
    y += 35f
    canvas.drawText("SERVICE VOUCHER", margin, y, headingPaint)
    y += 26f
    canvas.drawText("Voucher No: $voucherNo", margin, y, bodyPaint)
    canvas.drawText("Date: $dateText", 310f, y, bodyPaint)
    y += 30f

    canvas.drawText("Client", margin, y, headingPaint)
    y += 21f
    y = drawWrapped(clientName, margin, y, bodyPaint, contentWidth, 18f)
    if (clientPhone.trim().isNotBlank()) {
        canvas.drawText("Phone: ${clientPhone.trim()}", margin, y, bodyPaint)
        y += 22f
    }

    y += 10f
    canvas.drawLine(margin, y, 595f - margin, y, linePaint)
    y += 30f
    canvas.drawText("Work / Service", margin, y, headingPaint)
    y += 22f
    y = drawWrapped(transaction.category, margin, y, bodyPaint, contentWidth, 18f)

    if (workDetails.trim().isNotBlank()) {
        y += 6f
        y = drawWrapped(workDetails, margin, y, bodyPaint, contentWidth, 18f)
    }

    y += 25f
    canvas.drawLine(margin, y, 595f - margin, y, linePaint)
    y += 38f
    canvas.drawText("Amount Received", margin, y, headingPaint)
    y += 34f
    canvas.drawText("$currencySymbol $money", margin, y, amountPaint)
    y += 40f
    canvas.drawText("Payment Status: PAID", margin, y, headingPaint)
    y += 32f
    canvas.drawText("Received with thanks.", margin, y, bodyPaint)

    y += 75f
    canvas.drawLine(385f, y, 545f, y, linePaint)
    y += 18f
    canvas.drawText("Authorized Signature", 405f, y, mutedPaint)

    canvas.drawText("Generated by Hisabi Khata", margin, 792f, mutedPaint)
    canvas.drawText("Developed by MD Mezbah Uddin", margin, 810f, mutedPaint)

    document.finishPage(page)
    file.outputStream().use { document.writeTo(it) }
    document.close()

    BusinessVoucherPdf(file = file)
}
