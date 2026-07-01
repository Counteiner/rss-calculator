package com.rcalc.resourcecalculator.ocr

import com.rcalc.resourcecalculator.model.ResourceEntry

object GridShopParser {

    private val RESOURCE_NAMES = listOf("Food", "Wood", "Stone", "Gold")
    private val STANDARD_PACK_VALUES = setOf(10_000.0, 50_000.0, 150_000.0, 500_000.0)

    fun parse(
        blocks: List<OcrBlock>,
        imageWidth: Int,
        imageHeight: Int
    ): List<ResourceEntry> {
        val gridBlocks = blocks.filter { block ->
            block.left < imageWidth * 0.6 &&
            block.top > imageHeight * 0.10 &&
            block.bottom < imageHeight * 0.85
        }

        if (gridBlocks.isEmpty()) return emptyList()

        val rows = clusterByY(gridBlocks, 4)
        if (rows.size != 4) return emptyList()

        val results = mutableListOf<ResourceEntry>()
        for ((i, rowBlocks) in rows.withIndex()) {
            if (i >= RESOURCE_NAMES.size) break
            val total = computeRowTotal(rowBlocks)
            results.add(ResourceEntry(RESOURCE_NAMES[i], total, total))
        }

        return results
    }

    private fun clusterByY(blocks: List<OcrBlock>, k: Int): List<List<OcrBlock>> {
        if (blocks.isEmpty()) return emptyList()
        if (blocks.size <= k) return blocks.map { listOf(it) }

        val sorted = blocks.sortedBy { it.centerY }
        val gaps = sorted.zipWithNext { a, b -> b.centerY - a.centerY }
        if (gaps.isEmpty()) return listOf(sorted)

        val gapIndices = gaps.indices
            .sortedByDescending { gaps[it] }
            .take(k - 1)
            .sorted()

        val groups = mutableListOf<MutableList<OcrBlock>>()
        var start = 0
        for (splitIdx in gapIndices) {
            val group = mutableListOf<OcrBlock>()
            for (j in start..splitIdx) {
                group.add(sorted[j])
            }
            groups.add(group)
            start = splitIdx + 1
        }
        if (start < sorted.size) {
            val lastGroup = mutableListOf<OcrBlock>()
            for (j in start until sorted.size) {
                lastGroup.add(sorted[j])
            }
            groups.add(lastGroup)
        }

        return groups
    }

    private fun computeRowTotal(rowBlocks: List<OcrBlock>): Double {
        val sortedByX = rowBlocks.sortedBy { it.centerX }

        val extracted = sortedByX.flatMap { block ->
            ValueParser.findValues(block.text, 50.0)
        }
        if (extracted.isEmpty()) return 0.0

        val values = extracted.map { it.value }

        val roundOnes = mutableListOf<Double>()
        val smallOnes = mutableListOf<Double>()

        for (v in values) {
            if (v >= 500.0 && v % 1000 == 0.0) {
                roundOnes.add(v)
            } else {
                smallOnes.add(v)
            }
        }

        if (roundOnes.size >= 4 && smallOnes.size >= 4) {
            val sortedNominals = roundOnes.sorted()
            val sortedBadges = smallOnes.sortedDescending()
            return sortedNominals.zip(sortedBadges).sumOf { (nom, badge) ->
                nom * badge.toLong()
            }
        }

        val byHeight = rowBlocks.sortedByDescending { it.height }
        val bigVals = mutableListOf<Double>()
        val smallVals = mutableListOf<Double>()
        val medianHeight = if (byHeight.isNotEmpty()) byHeight[byHeight.size / 2].height else 30
        for (block in byHeight) {
            val vals = ValueParser.findValues(block.text, 50.0).map { it.value }
            if (block.height >= medianHeight) {
                bigVals.addAll(vals)
            } else {
                smallVals.addAll(vals)
            }
        }

        if (bigVals.size >= 4 && smallVals.size >= 4) {
            val sortedBig = bigVals.sorted()
            val sortedSmall = smallVals.sortedDescending()
            return sortedBig.zip(sortedSmall).sumOf { (nom, badge) ->
                nom * badge.toLong()
            }
        }

        if (extracted.size >= 8) {
            val sorted = extracted.sortedByDescending { it.value }
            val top4 = sorted.take(4).sortedBy { it.value }
            val bot4 = sorted.drop(4).take(4).sortedByDescending { it.value }
            return top4.zip(bot4).sumOf { (a, b) ->
                a.value * b.value.toLong()
            }
        }

        return 0.0
    }
}
