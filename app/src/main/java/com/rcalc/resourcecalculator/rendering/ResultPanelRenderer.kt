package com.rcalc.resourcecalculator.rendering

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import com.rcalc.resourcecalculator.model.ScanResult
import com.rcalc.resourcecalculator.ocr.ValueParser

object ResultPanelRenderer {

    fun appendResultPanel(original: Bitmap, result: ScanResult, format: String = "A"): Bitmap {
        val width = original.width
        val title = "HASIL PERHITUNGAN"

        val lines = if (format == "B") {
            listOf(
                title,
                "Grand Total: ${ValueParser.formatCompact(result.totalFromItems)}"
            )
        } else {
            listOf(
                title,
                "Total From Items: ${ValueParser.formatCompact(result.totalFromItems)}",
                "Total Resources : ${ValueParser.formatCompact(result.totalResources)}"
            )
        }

        val titlePaint = Paint().apply {
            color = android.graphics.Color.rgb(255, 215, 0)
            textSize = 30f * width / 720f
            isAntiAlias = true
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        val bodyPaint = Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 26f * width / 720f
            isAntiAlias = true
            typeface = Typeface.DEFAULT
            textAlign = Paint.Align.CENTER
        }

        val lineHeight = (42f * width / 720f).toInt()
        val padding = (30f * width / 720f).toInt()
        val panelHeight = padding * 2 + lineHeight * lines.size

        val newBitmap = Bitmap.createBitmap(width, original.height + panelHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(newBitmap)
        canvas.drawBitmap(original, 0f, 0f, null)
        val bgPaint = Paint().apply { color = android.graphics.Color.rgb(20, 24, 32) }
        canvas.drawRect(0f, original.height.toFloat(), width.toFloat(), newBitmap.height.toFloat(), bgPaint)

        var y = original.height + padding
        for ((i, line) in lines.withIndex()) {
            val paint = if (i == 0) titlePaint else bodyPaint
            canvas.drawText(line, (width / 2f).toInt().toFloat(), y.toFloat(), paint)
            y += lineHeight
        }
        return newBitmap
    }
}
