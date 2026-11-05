package com.example.myapplication.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.myapplication.model.PaymentMethod
import com.example.myapplication.model.SaleRecord
import com.example.myapplication.ui.formatMoney
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportGenerator {

    fun generateSalesReportPdf(
        context: Context,
        businessName: String,
        dateMillis: Long,
        sales: List<SaleRecord>,
        cashTotal: Double,
        transferTotal: Double,
        grandTotal: Double
    ): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size: 595 x 842 pt
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint()
        val titlePaint = Paint()
        val headerPaint = Paint()

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val dateString = dateFormat.format(Date(dateMillis))
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        val totalDiscounts = sales.sumOf { it.discountAmount }

        var y = 40f

        // Encabezado Principal
        titlePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        titlePaint.textSize = 20f
        titlePaint.color = Color.rgb(26, 35, 126) // Azul Oscuro
        canvas.drawText("Feriventas - Reporte de Ventas", 40f, y, titlePaint)

        y += 25f
        paint.textSize = 14f
        paint.color = Color.DKGRAY
        val displayName = businessName.ifBlank { "Feriventas" }
        canvas.drawText("Emprendimiento: $displayName", 40f, y, paint)

        y += 20f
        canvas.drawText("Fecha del reporte: $dateString", 40f, y, paint)

        // Línea divisoria
        y += 15f
        paint.color = Color.LTGRAY
        paint.strokeWidth = 1f
        canvas.drawLine(40f, y, 555f, y, paint)

        // Resumen
        y += 30f
        headerPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        headerPaint.textSize = 14f
        headerPaint.color = Color.BLACK
        canvas.drawText("Resumen General", 40f, y, headerPaint)

        y += 20f
        paint.textSize = 12f
        paint.color = Color.BLACK
        canvas.drawText("• Total Transacciones: ${sales.size}", 50f, y, paint)

        y += 18f
        canvas.drawText("• Ventas en Efectivo: ${formatMoney(cashTotal)}", 50f, y, paint)

        y += 18f
        canvas.drawText("• Ventas en Transferencia / MP: ${formatMoney(transferTotal)}", 50f, y, paint)

        if (totalDiscounts > 0) {
            y += 18f
            paint.color = Color.rgb(183, 28, 28) // Rojo oscuro
            canvas.drawText("• Total Descuentos Otorgados: -${formatMoney(totalDiscounts)}", 50f, y, paint)
            paint.color = Color.BLACK
        }

        y += 22f
        titlePaint.textSize = 15f
        titlePaint.color = Color.rgb(46, 125, 50) // Verde
        canvas.drawText("Total Recaudado: ${formatMoney(grandTotal)}", 50f, y, titlePaint)

        // Tabla de Transacciones
        y += 30f
        headerPaint.textSize = 14f
        headerPaint.color = Color.BLACK
        canvas.drawText("Detalle de Transacciones", 40f, y, headerPaint)

        y += 15f
        paint.color = Color.LTGRAY
        canvas.drawLine(40f, y, 555f, y, paint)

        y += 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 10f
        paint.color = Color.DKGRAY
        canvas.drawText("Hora", 40f, y, paint)
        canvas.drawText("Pago", 80f, y, paint)
        canvas.drawText("Detalle de Productos", 150f, y, paint)
        canvas.drawText("Subtotal", 410f, y, paint)
        canvas.drawText("Desc.", 475f, y, paint)
        canvas.drawText("Total", 520f, y, paint)

        y += 10f
        paint.color = Color.LTGRAY
        canvas.drawLine(40f, y, 555f, y, paint)

        paint.typeface = Typeface.DEFAULT
        paint.textSize = 9f
        paint.color = Color.BLACK

        val maxY = 800f

        sales.forEach { sale ->
            if (y > maxY) {
                return@forEach
            }
            y += 18f
            val timeStr = timeFormat.format(Date(sale.timestamp))
            val pmStr = if (sale.paymentMethod == PaymentMethod.CASH) "Efectivo" else "Transf."
            val itemsSummary = sale.items.joinToString(", ") { "${it.quantity}x ${it.productName}" }

            canvas.drawText(timeStr, 40f, y, paint)
            canvas.drawText(pmStr, 80f, y, paint)

            val truncatedSummary = if (itemsSummary.length > 40) itemsSummary.take(37) + "..." else itemsSummary
            canvas.drawText(truncatedSummary, 150f, y, paint)

            canvas.drawText(formatMoney(sale.subtotalAmount), 410f, y, paint)
            val descStr = if (sale.discountAmount > 0) "-${formatMoney(sale.discountAmount)}" else "-"
            canvas.drawText(descStr, 475f, y, paint)
            canvas.drawText(formatMoney(sale.totalAmount), 520f, y, paint)
        }

        pdfDocument.finishPage(page)

        return try {
            val cacheDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val sanitizedDate = dateString.replace("/", "-")
            val pdfFile = File(cacheDir, "reporte_ventas_$sanitizedDate.pdf")
            pdfDocument.writeTo(FileOutputStream(pdfFile))
            pdfDocument.close()
            pdfFile
        } catch (e: Exception) {
            pdfDocument.close()
            null
        }
    }
}
