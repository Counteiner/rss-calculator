package com.rcalc.resourcecalculator.ocr

import com.rcalc.resourcecalculator.model.ResourceEntry

object ResourceTableParser {

    private val RESOURCE_NAMES = listOf("Food", "Wood", "Stone", "Gold")

    fun parse(text: String, minValue: Double = 1000.0): List<ResourceEntry> {
        val combined = text.replace("\n", " ").replace("\r", " ")
        val pattern = Regex("""([A-Za-z]\w*)\s+([\d.,]+\s*[KMB]?)\s+([\d.,]+\s*[KMB]?)""")
        val matches = pattern.findAll(combined)

        data class Candidate(
            val word: String,
            val startIndex: Int,
            val fromItems: Double,
            val total: Double
        )

        val candidates = mutableListOf<Candidate>()

        for (m in matches) {
            val word = m.groupValues[1]
            val num1Raw = m.groupValues[2].trim()
            val num2Raw = m.groupValues[3].trim()

            val (num1Clean, suffix1) = splitNumSuffix(num1Raw)
            val (num2Clean, suffix2) = splitNumSuffix(num2Raw)

            val val1 = ValueParser.convertValue(num1Clean, suffix1)
            val val2 = ValueParser.convertValue(num2Clean, suffix2)

            if (val1 >= minValue && val2 >= minValue) {
                candidates.add(Candidate(word, m.range.first, val1, val2))
            }
        }

        val assigned = mutableMapOf<String, Candidate>()
        val used = mutableSetOf<Int>()

        for (resName in RESOURCE_NAMES) {
            var bestScore = Int.MAX_VALUE
            var bestIdx = -1
            var bestCandidate: Candidate? = null

            for ((i, c) in candidates.withIndex()) {
                if (i in used) continue
                val score = similarityScore(c.word, resName)
                if (score < bestScore) {
                    bestScore = score
                    bestIdx = i
                    bestCandidate = c
                }
            }

            if (bestCandidate != null && bestScore <= 2) {
                assigned[resName] = bestCandidate
                used.add(bestIdx)
            }
        }

        val unused = candidates.filterIndexed { i, _ -> i !in used }
            .sortedBy { it.startIndex }

        var unusedIdx = 0
        for (name in RESOURCE_NAMES) {
            if (name in assigned) continue
            if (unusedIdx < unused.size) {
                assigned[name] = unused[unusedIdx]
                unusedIdx++
            }
        }

        val unmatchedFallback = RESOURCE_NAMES.filter { it !in assigned }
        val allValues = ValueParser.findValues(combined, minValue)
        if (unmatchedFallback.isNotEmpty() && allValues.size >= 2 * RESOURCE_NAMES.size) {
            val usedIndices = assigned.values.mapNotNull { c ->
                val firstVal = allValues.indexOfFirst { kotlin.math.abs(it.value - c.fromItems) < 1.0 }
                if (firstVal >= 0) firstVal else -1
            }.filter { it >= 0 }.toMutableSet()

            for (name in unmatchedFallback) {
                var pairStart = -1
                for (i in allValues.indices step 2) {
                    if (i + 1 < allValues.size && i !in usedIndices && (i + 1) !in usedIndices) {
                        pairStart = i
                        break
                    }
                }
                if (pairStart >= 0 && pairStart + 1 < allValues.size) {
                    assigned[name] = Candidate(name, pairStart, allValues[pairStart].value, allValues[pairStart + 1].value)
                    usedIndices.add(pairStart)
                    usedIndices.add(pairStart + 1)
                }
            }
        }

        return RESOURCE_NAMES.mapNotNull { name ->
            assigned[name]?.let { ResourceEntry(name, it.fromItems, it.total) }
        }
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
