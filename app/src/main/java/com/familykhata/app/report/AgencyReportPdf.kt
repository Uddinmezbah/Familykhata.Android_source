package com.familykhata.app.report

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.familykhata.app.agency.AgencyProjectSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AgencyReportPdf(
    val file: File
)

suspend fun writeAgencyReportPdf(
    context: Context,
    projects: List<AgencyProjectSummary>
): AgencyReportPdf = withContext(Dispatchers.IO) {
    val dir =
        File(
            context.cacheDir,
            "agency_reports"
        ).apply {
            mkdirs()
        }

    val stamp =
        SimpleDateFormat(
            "yyyyMMdd-HHmmss",
            Locale.US
        ).format(Date())

    val file =
        File(
            dir,
            "HisabiKhata-Agency-Report-$stamp.pdf"
        )

    val document =
        PdfDocument()

    val titlePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color =
                android.graphics.Color.rgb(
                    38,
                    55,
                    78
                )
            textSize = 22f
            typeface =
                Typeface.create(
                    "sans-serif",
                    Typeface.BOLD
                )
        }

    val headingPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color =
                android.graphics.Color.rgb(
                    45,
                    65,
                    90
                )
            textSize = 13f
            typeface =
                Typeface.create(
                    "sans-serif",
                    Typeface.BOLD
                )
        }

    val bodyPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color =
                android.graphics.Color.rgb(
                    35,
                    40,
                    48
                )
            textSize = 11f
            typeface =
                Typeface.create(
                    "sans-serif",
                    Typeface.NORMAL
                )
        }

    val smallPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color =
                android.graphics.Color.rgb(
                    100,
                    108,
                    118
                )
            textSize = 9.5f
            typeface =
                Typeface.create(
                    "sans-serif",
                    Typeface.NORMAL
                )
        }

    val linePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color =
                android.graphics.Color.rgb(
                    218,
                    223,
                    230
                )
            strokeWidth = 1f
        }

    val money =
        DecimalFormat(
            "#,##0.##"
        )

    fun fit(
        text: String,
        paint: Paint,
        maxWidth: Float
    ): String {
        if (
            paint.measureText(text) <=
            maxWidth
        ) {
            return text
        }

        var value =
            text

        while (
            value.length > 3 &&
            paint.measureText(
                "$value…"
            ) >
            maxWidth
        ) {
            value =
                value.dropLast(1)
        }

        return "$value…"
    }

    var pageNumber = 0
    var page:
        PdfDocument.Page? = null
    var y = 0f

    fun startPage() {
        page?.let {
            document.finishPage(it)
        }

        pageNumber += 1

        page =
            document.startPage(
                PdfDocument.PageInfo.Builder(
                    595,
                    842,
                    pageNumber
                ).create()
            )

        val canvas =
            page!!.canvas

        canvas.drawText(
            "Hisabi Khata - Digital Agency Report",
            38f,
            52f,
            titlePaint
        )

        canvas.drawText(
            "Generated: " +
                SimpleDateFormat(
                    "dd MMM yyyy, hh:mm a",
                    Locale.getDefault()
                ).format(Date()),
            38f,
            73f,
            smallPaint
        )

        canvas.drawLine(
            38f,
            86f,
            557f,
            86f,
            linePaint
        )

        y = 108f
    }

    startPage()

    val totalCharges =
        projects.sumOf {
            it.totalPrice
        }

    val totalPaid =
        projects.sumOf {
            it.totalPaid
        }

    val totalDue =
        projects.sumOf {
            it.dueAmount
                .coerceAtLeast(0.0)
        }

    val active =
        projects.count {
            it.status ==
                "ACTIVE"
        }

    val completed =
        projects.count {
            it.status ==
                "COMPLETED"
        }

    page!!.canvas.apply {
        drawText(
            "Summary",
            38f,
            y,
            headingPaint
        )

        y += 21f

        drawText(
            "Projects: ${projects.size}   Active: $active   Completed: $completed",
            38f,
            y,
            bodyPaint
        )

        y += 18f

        drawText(
            "Total charges: ${money.format(totalCharges)}   Paid: ${money.format(totalPaid)}   Due: ${money.format(totalDue)}",
            38f,
            y,
            bodyPaint
        )

        y += 28f

        drawLine(
            38f,
            y,
            557f,
            y,
            linePaint
        )

        y += 23f

        drawText(
            "Projects / Clients",
            38f,
            y,
            headingPaint
        )

        y += 21f
    }

    if (projects.isEmpty()) {
        page!!.canvas.drawText(
            "No agency projects found.",
            38f,
            y,
            bodyPaint
        )
    } else {
        projects.forEachIndexed {
                index,
                item ->

            if (y > 730f) {
                page!!.canvas.drawText(
                    "Page $pageNumber",
                    505f,
                    810f,
                    smallPaint
                )

                startPage()
            }

            val canvas =
                page!!.canvas

            canvas.drawText(
                fit(
                    "${index + 1}. ${item.clientName} - ${item.title}",
                    headingPaint,
                    515f
                ),
                38f,
                y,
                headingPaint
            )

            y += 17f

            canvas.drawText(
                fit(
                    "Service: ${item.serviceType.ifBlank { "-" }}   Status: ${item.status}",
                    bodyPaint,
                    515f
                ),
                50f,
                y,
                bodyPaint
            )

            y += 16f

            canvas.drawText(
                "Charge: ${money.format(item.totalPrice)}   Paid: ${money.format(item.totalPaid)}   Due: ${money.format(item.dueAmount.coerceAtLeast(0.0))}",
                50f,
                y,
                bodyPaint
            )

            y += 18f

            if (
                item.company.isNotBlank()
            ) {
                canvas.drawText(
                    fit(
                        "Company: ${item.company}",
                        smallPaint,
                        500f
                    ),
                    50f,
                    y,
                    smallPaint
                )

                y += 14f
            }

            canvas.drawLine(
                50f,
                y,
                557f,
                y,
                linePaint
            )

            y += 17f
        }
    }

    page!!.canvas.drawText(
        "Developed by MD Mezbah Uddin",
        38f,
        810f,
        smallPaint
    )

    page!!.canvas.drawText(
        "Page $pageNumber",
        505f,
        810f,
        smallPaint
    )

    page?.let {
        document.finishPage(it)
    }

    file.outputStream().use {
        document.writeTo(it)
    }

    document.close()

    AgencyReportPdf(
        file = file
    )
}
