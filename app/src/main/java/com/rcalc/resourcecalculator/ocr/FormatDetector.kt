package com.rcalc.resourcecalculator.ocr

enum class OcrFormat { FORMAT_A, FORMAT_B, UNKNOWN }

object FormatDetector {

    fun detect(rawText: String): OcrFormat {
        val lower = rawText.lowercase()

        if (lower.contains("from items")) {
            return FORMAT_A
        }

        val values = ValueParser.findValues(rawText, 1000.0)
        if (values.size >= 8) {
            if (lower.contains("resource pack") ||
                lower.contains("owned") ||
                lower.contains("speedups") ||
                lower.contains("boosts") ||
                lower.contains("statistics")
            ) {
                return FORMAT_B
            }
            return FORMAT_A
        }

        return UNKNOWN
    }
}
