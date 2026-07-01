package com.rcalc.resourcecalculator.ocr

import com.rcalc.resourcecalculator.model.ResourceEntry

object ResourceTableParser {

    private val RESOURCE_NAMES = listOf("Food", "Wood", "Stone", "Gold")

    fun parse(text: String, minValue: Double = 1000.0): List<ResourceEntry> {
        val combined = text.replace("\n", " ").replace("\r", " ")

        val candidates = extractCandidates(combined, minValue)

        val matched = matchToResources(candidates)
        val remaining = RESOURCE_NAMES.filter { it !in matched }

        if (remaining.isNotEmpty()) {
            val fallback = fallbackPairValues(combined, minValue, matched)
            matched.putAll(fallback)
        }

        return RESOURCE_NAMES.mapNotNull { name ->
            matched[name]?.let { ResourceEntry(name, it.first, it.second) }
        }
    }

    private data class Candidate(
        val word: String,
        val startIndex: Int,
        val fromItems: Double,
        val total: Double
    )

    private fun extractCandidates(text: String, minValue: Double): List<Candidate> {
        val pattern = Regex("""([A-Za-z]\w*)\W+([\d.,]+\s*[KMB]?)\W+([\d.,]+\s*[KMB]?)""")
        val matches = pattern.findAll(text)
        val result = mutableListOf<Candidate>()

        for (m in matches) {
            val word = m.groupValues[1]
            val (val1, val2) = parseTwoNums(m.groupValues[2], m.groupValues[3])
            if (val1 >= minValue && val2 >= minValue) {
                result.add(Candidate(word, m.range.first, val1, val2))
            }
        }
        return result
    }

    private fun parseTwoNums(raw1: String, raw2: String): Pair<Double, Double> {
        val (n1, s1) = splitNumSuffix(raw1.trim())
        val (n2, s2) = splitNumSuffix(raw2.trim())
        return Pair(
            ValueParser.convertValue(n1, s1),
            ValueParser.convertValue(n2, s2)
        )
    }

    private fun matchToResources(candidates: List<Candidate>): MutableMap<String, Pair<Double, Double>> {
        val assigned = mutableMapOf<String, Pair<Double, Double>>()
        val used = mutableSetOf<Int>()

        for (resName in RESOURCE_NAMES) {
            var bestScore = Int.MAX_VALUE
            var bestIdx = -1
            for ((i, c) in candidates.withIndex()) {
                if (i in used) continue
                val score = similarityScore(c.word, resName)
                if (score < bestScore) {
                    bestScore = score
                    bestIdx = i
                }
            }
            if (bestIdx >= 0 && bestScore <= 2) {
                val c = candidates[bestIdx]
                assigned[resName] = Pair(c.fromItems, c.total)
                used.add(bestIdx)
            }
        }

        val unused = candidates.filterIndexed { i, _ -> i !in used }
            .sortedBy { it.startIndex }
        var idx = 0
        for (name in RESOURCE_NAMES) {
            if (name in assigned) continue
            if (idx < unused.size) {
                val c = unused[idx]
                assigned[name] = Pair(c.fromItems, c.total)
                idx++
            }
        }

        return assigned
    }

    private fun fallbackPairValues(
        text: String,
        minValue: Double,
        existing: Map<String, Pair<Double, Double>>
    ): Map<String, Pair<Double, Double>> {
        val allValues = ValueParser.findValues(text, minValue)
        if (allValues.size < 8) {
            val csvFallback = extractCsvNumbers(text, minValue)
            if (csvFallback.size >= 8) {
                return assignPairs(csvFallback, existing)
            }
            return emptyMap()
        }
        return assignPairs(allValues.map { it.value }, existing)
    }

    private fun extractCsvNumbers(text: String, minValue: Double): List<Double> {
        val pattern = Regex("""[\d,]+\.?\d*""")
        val matches = pattern.findAll(text)
        return matches.mapNotNull { m ->
            val clean = m.value.replace(",", "")
            val num = clean.toDoubleOrNull()
            if (num != null && num >= minValue) num else null
        }.toList()
    }

    private fun assignPairs(
        values: List<Double>,
        existing: Map<String, Pair<Double, Double>>
    ): Map<String, Pair<Double, Double>> {
        val result = mutableMapOf<String, Pair<Double, Double>>()
        val used = mutableSetOf<Int>()

        val existingPairs = existing.values.map { it.first }
        for (v in existingPairs) {
            val idx = values.indexOfFirst { kotlin.math.abs(it - v) < 1.0 }
            if (idx >= 0) { used.add(idx); if (idx + 1 < values.size) used.add(idx + 1) }
        }

        var vi = 0
        for (name in RESOURCE_NAMES) {
            if (name in existing) {
                result[name] = existing[name]!!
                continue
            }
            while (vi < values.size - 1) {
                if (vi !in used && (vi + 1) !in used) {
                    result[name] = Pair(values[vi], values[vi + 1])
                    used.add(vi); used.add(vi + 1)
                    vi += 2
                    break
                }
                vi++
            }
        }
        return result
    }

    private fun splitNumSuffix(raw: String): Pair<String, String> {
        val clean = raw.replace(",", "")
        val suffix = clean.lastOrNull { it in "KMBkmb" }?.uppercase() ?: ""
        val numPart = if (suffix.isNotEmpty()) clean.dropLast(1).trimEnd() else clean
        return Pair(numPart, suffix)
    }

    private fun similarityScore(ocr: String, target: String): Int {
        val a = ocr.lowercase()
        val b = target.lowercase()
        val m = a.length
        val n = b.length
        val dp = Array(m + 1) { IntArray(n + 1) }
        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j
        for (i in 1..m) {
            for (j in 1..n) {
                val cost = if (charsMatch(a[i - 1], b[j - 1])) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[m][n]
    }

    private fun charsMatch(c1: Char, c2: Char): Boolean {
        if (c1 == c2) return true
        return when (c1.lowercaseChar()) {
            '0' -> c2.lowercaseChar() == 'o'
            'o' -> c2.lowercaseChar() == '0'
            '1' -> c2.lowercaseChar() == 'l' || c2.lowercaseChar() == 'i'
            'l' -> c2.lowercaseChar() == '1' || c2.lowercaseChar() == 'i'
            'i' -> c2.lowercaseChar() == 'l' || c2.lowercaseChar() == '1'
            '5' -> c2.lowercaseChar() == 's'
            's' -> c2.lowercaseChar() == '5'
            else -> false
        }
    }
}
