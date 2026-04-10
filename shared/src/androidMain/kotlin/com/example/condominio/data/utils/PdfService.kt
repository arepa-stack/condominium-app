package com.example.condominio.data.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.condominio.data.model.Payment
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale


class PdfServiceImpl (
    private val context: android.content.Context
) : com.example.condominio.data.utils.PdfService {

    override fun generateReceipt(payment: Payment): String {
        val pdfDocument = android.graphics.pdf.PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size in points (approx)
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val paint = Paint()

        // Formatting
        val dateFormat = SimpleDateFormat("MMM dd, yyyy, h:mm a", Locale.getDefault())
        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val labelPaint = Paint().apply {
            color = Color.GRAY
            textSize = 14f
        }
        val valuePaint = Paint().apply {
            color = Color.BLACK
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        var y = 50f
        val x = 40f
        val xValue = 200f

        // Header
        canvas.drawText("Payment Receipt", x, y, titlePaint)
        y += 40f

        paint.color = Color.LTGRAY
        paint.strokeWidth = 2f
        canvas.drawLine(x, y, pageInfo.pageWidth - x, y, paint)
        y += 30f

        // Details
        fun drawRow(label: String, value: String) {
            canvas.drawText(label, x, y, labelPaint)
            canvas.drawText(value, xValue, y, valuePaint)
            y += 25f
        }

        drawRow("Transaction ID:", "#${payment.id}")
        drawRow("Date:", dateFormat.format(payment.date))
        drawRow("Description:", payment.description)
        drawRow("Amount:", "$${String.format("%.2f", payment.amount)}")
        drawRow("Status:", payment.status.name)
        drawRow("Method:", payment.method.label)

        payment.bank?.let { drawRow("Bank:", it) }
        payment.reference?.let { drawRow("Reference:", it) }
        payment.phone?.let { drawRow("Phone:", it) }

        y += 20f
        paint.color = Color.LTGRAY
        canvas.drawLine(x, y, pageInfo.pageWidth - x, y, paint)
        y += 30f

        // Footer
        val footerPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 12f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Thank you for your payment.", pageInfo.pageWidth / 2f, y, footerPaint)

        pdfDocument.finishPage(page)

        // Save file
        val file = File(context.cacheDir, "receipt_${payment.id}.pdf")
        try {
            pdfDocument.writeTo(FileOutputStream(file))
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            pdfDocument.close()
        }

        return file.absolutePath
    }
}
