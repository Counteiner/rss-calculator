package com.rcalc.resourcecalculator.ocr

import com.rcalc.resourcecalculator.model.ResourceEntry

object ResourceTableParser {

    private val RESOURCE_NAMES = listOf("Food", "Wood", "Stone", "Gold")

    fun parse(text: String, minValue: Double = 1000.0): List<ResourceEntry> {
        val rows = mutableListOf<ResourceEntry>()
        for (line in text.lines()) {
            val nums = ValueParser.findValues(line, minValue)
            if (nums.size < 2) continue
            val words = Regex("[A-Za-z]+").findAll(line)
                .map { it.value }
                .filter { it.length > 2 }
                .toList()
            if (words.isEmpty()) continue
            val name = words.last()
            val matched = RESOURCE_NAMES.any { name.contains(it, ignoreCase = true) }
            if (!matched) continue
            val fromItems = nums[nums.size - 2].value
            val total = nums.last().value
            rows.add(ResourceEntry(name = name.replaceFirstChar { it.uppercase() }, fromItems = fromItems, total = total))
        }
        val resourceOrder = listOf("Food", "Wood", "Stone", "Gold")
        return rows.sortedBy { r -> resourceOrder.indexOf(r.name).let { if (it < 0) Int.MAX_VALUE else it } }
    }
}
