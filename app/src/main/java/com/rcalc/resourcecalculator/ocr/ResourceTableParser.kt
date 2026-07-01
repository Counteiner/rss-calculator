package com.rcalc.resourcecalculator.ocr

import com.rcalc.resourcecalculator.model.ResourceEntry

object ResourceTableParser {

    private val RESOURCE_NAMES = listOf("Food", "Wood", "Stone", "Gold")

    fun parse(text: String, minValue: Double = 0.0): List<ResourceEntry> {
        val combined = text.replace("\n", " ").replace("\r", " ")

        val totalValues = extractSection(combined, "Total Resources", "Note")
        val fromValues = extractSection(combined, "From Items", "SPEEDUPS")

        if (totalValues.size >= 4) {
            val matched = matchFromToTotal(fromValues, totalValues)
            if (matched.size == 4) {
                return matched.mapIndexed { i, pair ->
                    ResourceEntry(RESOURCE_NAMES[i], pair.first, pair.second)
                }
            }
        }

        if (fromValues.size >= 8) {
            val pairs = fromValues.chunked(2).filter { it.size == 2 }.take(4)
            return pairs.mapIndexed { i, (a, b) ->
                ResourceEntry(RESOURCE_NAMES[i], a, b)
            }
        }

        val allValues = ValueParser.findValues(combined, 0.0).map { it.value }
            .filter { it >= 1000.0 }
        if (allValues.size >= 8) {
            val pairs = allValues.chunked(2).filter { it.size == 2 }.take(4)
            return pairs.mapIndexed { i, (a, b) ->
                ResourceEntry(RESOURCE_NAMES[i], a, b)
            }
        }

        return emptyList()
    }

    private fun extractSection(text: String, startAnchor: String, endAnchor: String): List<Double> {
        val start = text.indexOf(startAnchor)
        if (start < 0) return emptyList()
        val searchFrom = start + startAnchor.length
        val end = if (endAnchor.isNotEmpty()) {
            val idx = text.indexOf(endAnchor, searchFrom)
            if (idx < 0) text.length else idx
        } else {
            text.length
        }
        if (end <= searchFrom) return emptyList()
        val section = text.substring(searchFrom, end)
        return ValueParser.findValues(section, 0.0).map { it.value }
    }

    private fun matchFromToTotal(fromValues: List<Double>, totalValues: List<Double>): List<Pair<Double, Double>> {
        val used = mutableSetOf<Int>()
        val result = mutableListOf<Pair<Double, Double>>()

        for (total in totalValues.take(4)) {
            var bestVal = 0.0
            var bestDiff = Double.MAX_VALUE
            var bestIdx = -1

            for ((i, fv) in fromValues.withIndex()) {
                if (i in used) continue
                if (fv <= total) {
                    val diff = total - fv
                    if (diff < bestDiff) {
                        bestDiff = diff
                        bestVal = fv
                        bestIdx = i
                    }
                }
            }

            if (bestIdx >= 0) {
                used.add(bestIdx)
                result.add(Pair(bestVal, total))
            }
        }

        return result
    }
}
