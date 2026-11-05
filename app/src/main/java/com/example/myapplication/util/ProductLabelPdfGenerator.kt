package com.example.myapplication.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.myapplication.model.Product
import com.example.myapplication.ui.formatMoney
import java.io.File
import java.io.FileOutputStream

enum class LabelSize(val displayName: String, val widthPt: Int, val heightPt: Int) {
    SMALL("Chica (50x30 mm)", 142, 85),
    MEDIUM("Mediana (70x40 mm)", 198, 113),
    FULL_A4("Hoja A4 (Múltiples Etiquetas)", 595, 842)
}

object ProductLabelPdfGenerator {

    fun generateProductLabelPdf(
        context: Context,
        businessName: String,
        product: Product,
        labelSize: LabelSize
    ): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(labelSize.widthPt, labelSize.heightPt, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        if (labelSize == LabelSize.FULL_A4) {
            drawFullSheetGrid(canvas, businessName, product)
        } else {
            drawSingleLabel(canvas, labelSize.widthPt.toFloat(), labelSize.heightPt.toFloat(), businessName, product)
        }

        pdfDocument.finishPage(page)

        return try {
            val cacheDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val sanitizedName = product.name.replace(Regex("[^a-zA-Z0-9]"), "_")
            val pdfFile = File(cacheDir, "etiqueta_$sanitizedName.pdf")
            pdfDocument.writeTo(FileOutputStream(pdfFile))
            pdfDocument.close()
            pdfFile
        } catch (e: Exception) {
            pdfDocument.close()
            null
        }
    }

    fun generateMultiProductA4Pdf(
        context: Context,
        businessName: String,
        labelItems: List<Pair<Product, Int>>
    ): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(LabelSize.FULL_A4.widthPt, LabelSize.FULL_A4.heightPt, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val flattenedProducts = mutableListOf<Product>()
        labelItems.forEach { (prod, qty) ->
            repeat(qty) {
                flattenedProducts.add(prod)
            }
        }

        val labelW = 170f
        val labelH = 100f
        val startX = 30f
        val startY = 40f
        val cols = 3
        val rows = 7

        var itemIdx = 0
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                if (itemIdx < flattenedProducts.size) {
                    val product = flattenedProducts[itemIdx]
                    val x = startX + c * (labelW + 15f)
                    val y = startY + r * (labelH + 12f)
                    canvas.save()
                    canvas.translate(x, y)
                    drawSingleLabel(canvas, labelW, labelH, businessName, product)
                    canvas.restore()
                    itemIdx++
                }
            }
        }

        pdfDocument.finishPage(page)

        return try {
            val cacheDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val pdfFile = File(cacheDir, "etiquetas_A4_multiples.pdf")
            pdfDocument.writeTo(FileOutputStream(pdfFile))
            pdfDocument.close()
            pdfFile
        } catch (e: Exception) {
            pdfDocument.close()
            null
        }
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.isEmpty()) return emptyList()

        val lines = mutableListOf<String>()
        var currentLine = ""

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(testLine) <= maxWidth) {
                currentLine = testLine
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine)
                }
                if (paint.measureText(word) > maxWidth) {
                    var truncated = word
                    while (truncated.isNotEmpty() && paint.measureText("$truncated...") > maxWidth) {
                        truncated = truncated.dropLast(1)
                    }
                    lines.add(if (truncated.isNotEmpty()) "$truncated..." else word)
                    currentLine = ""
                } else {
                    currentLine = word
                }
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }
        return lines
    }

    private fun drawSingleLabel(
        canvas: Canvas,
        w: Float,
        h: Float,
        businessName: String,
        product: Product
    ) {
        val paint = Paint().apply { isAntiAlias = true }
        val borderPaint = Paint().apply {
            color = Color.LTGRAY
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        canvas.drawRect(2f, 2f, w - 2f, h - 2f, borderPaint)
        val maxTextWidth = w - 20f

        // Nombre del Emprendimiento (Top)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = (h * 0.12f).coerceIn(10f, 15f)
        paint.color = Color.rgb(26, 35, 126)
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(businessName.ifBlank { "Feriventas" }, w / 2f, h * 0.18f, paint)

        // Nombre del Producto (Centrado y Multilínea)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        var baseFontSize = (h * 0.18f).coerceIn(13f, 20f)
        paint.textSize = baseFontSize
        paint.color = Color.BLACK
        paint.textAlign = Paint.Align.CENTER

        var lines = wrapText(product.name, paint, maxTextWidth)
        if (lines.size > 2) {
            baseFontSize *= 0.82f
            paint.textSize = baseFontSize
            lines = wrapText(product.name, paint, maxTextWidth).take(3)
        }

        val lineHeight = baseFontSize * 1.15f
        var startY = if (lines.size == 1) {
            h * 0.46f
        } else if (lines.size == 2) {
            h * 0.38f
        } else {
            h * 0.32f
        }

        lines.forEach { line ->
            canvas.drawText(line, w / 2f, startY, paint)
            startY += lineHeight
        }

        // Precio (Bottom)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = (h * 0.22f).coerceIn(16f, 26f)
        paint.color = Color.rgb(46, 125, 50)
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(formatMoney(product.price), w / 2f, h * 0.85f, paint)
    }

    private fun drawFullSheetGrid(
        canvas: Canvas,
        businessName: String,
        product: Product
    ) {
        val labelW = 170f
        val labelH = 100f
        val startX = 30f
        val startY = 40f
        val cols = 3
        val rows = 7

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val x = startX + c * (labelW + 15f)
                val y = startY + r * (labelH + 12f)
                canvas.save()
                canvas.translate(x, y)
                drawSingleLabel(canvas, labelW, labelH, businessName, product)
                canvas.restore()
            }
        }
    }
}
