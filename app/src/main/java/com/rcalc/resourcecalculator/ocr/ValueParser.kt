package com.rcalc.resourcecalculator.ocr

data class ParsedValue(
    val raw: String,
    val value: Double
)

object ValueParser {

    fun convertValue(numStr: String, suffix: String): Double {
        val num = numStr.toDoubleOrNull() ?: return 0.0
        val multiplier = when (suffix.uppercase()) {
            "K" -> 1_000.0
            "M" -> 1_000_000.0
            "B" -> 1_000_000_000.0
            else -> 1.0
        }
        return num * multiplier
    }

    fun findValues(text: String, minValue: Double = 0.0): List<ParsedValue> {
        val pattern = Regex("""(\d[\d,]*\.?\d*)\s*([KMB]?)\b""")
        val matches = pattern.findAll(text)
        val results = mutableListOf<ParsedValue>()
        for (match in matches) {
            val (numRaw, suffix) = match.destructured
            val clean = numRaw.replace(",", "")
            if (clean.isEmpty() || clean == ".") continue
            val value = convertValue(clean, suffix)
            if (value >= minValue) {
                results.add(ParsedValue(raw = numRaw + suffix, value = value))
            }
        }
        return results
    }

    fun formatCompact(n: Double): String {
        val absN = kotlin.math.abs(n)
        return when {
            absN >= 1_000_000_000 -> {
                String.format("%.2f", n / 1_000_000_000)
                    .trimEnd('0').trimEnd('.') + "B"
            }
            absN >= 1_000_000 -> {
                String.format("%.2f", n / 1_000_000)
                    .trimEnd('0').trimEnd('.') + "M"
            }
            absN >= 1_000 -> {
                String.format("%.2f", n / 1_000)
                    .trimEnd('0').trimEnd('.') + "K"
            }
            else -> String.format("%.0f", n)
        }
    }
}
