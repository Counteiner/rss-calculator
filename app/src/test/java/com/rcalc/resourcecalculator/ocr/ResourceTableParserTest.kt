package com.rcalc.resourcecalculator.ocr

import org.junit.Assert.assertEquals
import org.junit.Test

class ResourceTableParserTest {

    @Test
    fun testParseStandardTable() {
        val text = "Food  19.3M  19.5M\nWood  20.4M  21.9M\nStone 5.8M   6.1M\nGold  1.1M   3.0M"
        val rows = ResourceTableParser.parse(text)
        assertEquals(4, rows.size)
        assertEquals("Food", rows[0].name)
        assertEquals("Wood", rows[1].name)
        assertEquals("Stone", rows[2].name)
        assertEquals("Gold", rows[3].name)
    }

    @Test
    fun testParseOrderedByResource() {
        val text = "Gold  1.1M   3.0M\nFood  19.3M  19.5M"
        val rows = ResourceTableParser.parse(text)
        assertEquals(2, rows.size)
        assertEquals("Food", rows[0].name)
        assertEquals("Gold", rows[1].name)
    }

    @Test
    fun testParseHandlesOcrTypos() {
        val text = "G0ld  1.1M   3.0M\nFo0d  19.3M  19.5M"
        val rows = ResourceTableParser.parse(text)
        assertEquals(2, rows.size)
    }

    @Test
    fun testParseRealOcrOutput() {
        val text = "Estimating resolution as 283Resource Type From Items Total Resources' Food 23.0M 23.7M@ Wood 22.8M 23.5M& Sstone 5.9M 9.7M@ ocou 15M 3.1M"
        val rows = ResourceTableParser.parse(text)
        assertEquals(4, rows.size)
        assertEquals("Food", rows[0].name)
        assertEquals("Wood", rows[1].name)
        assertEquals("Stone", rows[2].name)
        assertEquals("Gold", rows[3].name)
    }

    @Test
    fun testParseRealOcrTotals() {
        val text = "Food 23.0M 23.7M Wood 22.8M 23.5M Stone 5.9M 9.7M Gold 1.5M 3.1M"
        val rows = ResourceTableParser.parse(text)
        assertEquals(4, rows.size)
        val totalFromItems = rows.sumOf { it.fromItems }
        val totalResources = rows.sumOf { it.total }
        assertEquals(4, rows.size)
    }
}
