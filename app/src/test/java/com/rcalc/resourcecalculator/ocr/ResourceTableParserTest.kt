package com.rcalc.resourcecalculator.ocr

import org.junit.Assert.assertEquals
import org.junit.Test

class ResourceTableParserTest {

    @Test
    fun testParseStandardTable() {
        val text = "Food 19.3M 19.5M Wood 20.4M 21.9M Stone 5.8M 6.1M Gold 1.1M 3.0M"
        val rows = ResourceTableParser.parse(text)
        assertEquals(4, rows.size)
    }

    @Test
    fun testParseFromItemsAndTotal() {
        val text = "From Items 19.3M 20.4M 5.8M 1.1M Total Resources 19.5M 21.9M 6.1M 3.0M"
        val rows = ResourceTableParser.parse(text)
        assertEquals(4, rows.size)
        assertEquals(19_300_000.0, rows[0].fromItems, 0.001)
        assertEquals(19_500_000.0, rows[0].total, 0.001)
    }

    @Test
    fun testParseRealMLKitOutput() {
        val text = "Food 23.0M 22.8M 5.9M Stone Gold YOUR RESOURCES & SPEEDUPS RESOURCES From Items 23.0M 22.8M 5.9M 753.0K 730.6K 3.7M 1.5M SPEEDUPS Total Resources 23.7M 23.5M 9.7M 3.1M Note"
        val rows = ResourceTableParser.parse(text)
        assertEquals(4, rows.size)
        assertEquals(23_000_000.0, rows[0].fromItems, 0.001)
        assertEquals(23_700_000.0, rows[0].total, 0.001)
        assertEquals(22_800_000.0, rows[1].fromItems, 0.001)
        assertEquals(23_500_000.0, rows[1].total, 0.001)
        assertEquals(5_900_000.0, rows[2].fromItems, 0.001)
        assertEquals(9_700_000.0, rows[2].total, 0.001)
        assertEquals(1_500_000.0, rows[3].fromItems, 0.001)
        assertEquals(3_100_000.0, rows[3].total, 0.001)
    }

    @Test
    fun testParseWithKValues() {
        val text = "From Items 468K 22.8M 5.9M 1.5M SPEEDUPS Total Resources 500K 23.5M 9.7M 3.1M"
        val rows = ResourceTableParser.parse(text)
        assertEquals(4, rows.size)
        assertEquals(468_000.0, rows[0].fromItems, 0.001)
        assertEquals(500_000.0, rows[0].total, 0.001)
    }
}
