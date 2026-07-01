package com.rcalc.resourcecalculator.ocr

import com.rcalc.resourcecalculator.model.ResourceEntry
import kotlin.math.abs

object GridShopParser {

    private val RESOURCE_NAMES = listOf("Food", "Wood", "Stone", "Gold")

    fun parse(
        blocks: List<OcrBlock>,
        imageWidth: Int,
        imageHeight: Int
    ): List<ResourceEntry> {
        val gridBlocks = blocks.filter { it.left < imageWidth * 0.6 }
        if (gridBlocks.size < 8) {
            return emptyList()
        }

        val yVals = gridBlocks.map { it.centerY }.sorted()
        val q10 = percentile(yVals, 10)
        val q90 = percentile(yVals, 90)
        val middle = gridBlocks.filter { it.centerY in q10..q90 }

        val rowGroups = kMeans(middle, 4) { it.centerY.toDouble() }
        if (rowGroups.size != 4) {
            return emptyList()
        }
        val sortedRows = rowGroups.sortedBy { row ->
            row.sumOf { it.centerY } / row.size
        }

        val results = mutableListOf<ResourceEntry>()
        for ((idx, rowBlocks) in sortedRows.withIndex()) {
            if (idx >= RESOURCE_NAMES.size) break

            val colGroups = kMeans(rowBlocks, 4) { it.centerX.toDouble() }
            if (colGroups.size != 4) {
                results.add(ResourceEntry(RESOURCE_NAMES[idx], 0.0, 0.0))
                continue
            }
            val sortedCols = colGroups.sortedBy { col ->
                col.sumOf { it.centerX } / col.size
            }

            var rowTotal = 0.0
            for ((_, cell) in sortedCols.withIndex()) {
                val cellSum = extractCellTotal(cell)
                rowTotal += cellSum
            }
            results.add(ResourceEntry(RESOURCE_NAMES[idx], rowTotal, rowTotal))
        }

        return results
    }

    private fun extractCellTotal(cell: List<OcrBlock>): Double {
        val allValues = cell.flatMap { block ->
            ValueParser.findValues(block.text, 50.0)
        }
        if (allValues.isEmpty()) return 0.0

        if (allValues.size == 1) {
            return 0.0
        }

        val sortedByHeight = cell.sortedByDescending { it.height }
        val tallVals = ValueParser.findValues(sortedByHeight.first().text, 50.0)

        if (sortedByHeight.size >= 2 && tallVals.isNotEmpty()) {
            val shortVals = ValueParser.findValues(sortedByHeight.last().text, 50.0)
            if (shortVals.isNotEmpty()) {
                val numbers = listOf(tallVals.first().value, shortVals.first().value)
                val nominal = numbers.max()
                val badge = numbers.min()
                if (badge >= 1.0) {
                    return nominal * badge.toLong()
                }
            }
        }

        if (allValues.size >= 2) {
            val sorted = allValues.sortedByDescending { it.value }
            val large = sorted[0].value
            val small = sorted[1].value
            if (small >= 1.0) {
                return large * small.toLong()
            }
        }

        return 0.0
    }

    private fun kMeans(
        items: List<OcrBlock>,
        k: Int,
        extractor: (OcrBlock) -> Double
    ): List<List<OcrBlock>> {
        if (items.isEmpty()) return emptyList()
        if (items.size <= k) return items.map { listOf(it) }

        val values = items.map(extractor)
        val min = values.min()
        val max = values.max()
        val range = max - min
        val eps = 1e-6

        val centroids = (0 until k).map { i ->
            min + range * (i + 0.5) / k
        }.toMutableList()

        val assignments = MutableList(items.size) { -1 }

        for (iter in 0 until 100) {
            var changed = false

            for (i in items.indices) {
                var bestIdx = 0
                var bestDist = Double.MAX_VALUE
                for (j in 0 until k) {
                    val d = abs(values[i] - centroids[j])
                    if (d < bestDist) {
                        bestDist = d
                        bestIdx = j
                    }
                }
                if (assignments[i] != bestIdx) {
                    assignments[i] = bestIdx
                    changed = true
                }
            }

            if (!changed) break

            for (j in 0 until k) {
                val cluster = items.indices.filter { assignments[it] == j }
                if (cluster.isNotEmpty()) {
                    centroids[j] = cluster.map { values[it] }.average()
                }
            }
        }

        return (0 until k).map { j ->
            items.indices.filter { assignments[it] == j }.map { items[it] }
        }.filter { it.isNotEmpty() }
    }

    private fun percentile(sorted: List<Int>, p: Int): Int {
        if (sorted.isEmpty()) return 0
        val idx = (sorted.size - 1) * p / 100
        return sorted[idx.coerceIn(0, sorted.size - 1)]
    }
}
