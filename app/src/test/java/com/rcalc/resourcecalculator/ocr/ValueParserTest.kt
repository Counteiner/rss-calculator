package com.rcalc.resourcecalculator.ocr

import org.junit.Assert.assertEquals
import org.junit.Test

class ValueParserTest {

    @Test
    fun testConvertValue_noSuffix() {
        assertEquals(500.0, ValueParser.convertValue("500", ""), 0.001)
    }

    @Test
    fun testConvertValue_K() {
        assertEquals(5000.0, ValueParser.convertValue("5", "K"), 0.001)
    }

    @Test
    fun testConvertValue_M() {
        assertEquals(5_000_000.0, ValueParser.convertValue("5", "M"), 0.001)
    }

    @Test
    fun testConvertValue_B() {
        assertEquals(5_000_000_000.0, ValueParser.convertValue("5", "B"), 0.001)
    }

    @Test
    fun testConvertValue_decimal() {
        assertEquals(19_300_000.0, ValueParser.convertValue("19.3", "M"), 0.001)
    }

    @Test
    fun testFindValues() {
        val text = "Food 19.3M 19.5M\nWood 20.4M 21.9M"
        val results = ValueParser.findValues(text)
        assertEquals(4, results.size)
        assertEquals(19_300_000.0, results[0].value, 0.001)
        assertEquals(19_500_000.0, results[1].value, 0.001)
        assertEquals(20_400_000.0, results[2].value, 0.001)
        assertEquals(21_900_000.0, results[3].value, 0.001)
    }

    @Test
    fun testFindValues_withComma() {
        val text = "764,942"
        val results = ValueParser.findValues(text)
        assertEquals(1, results.size)
        assertEquals(764942.0, results[0].value, 0.001)
    }

    @Test
    fun testFormatCompact_M() {
        assertEquals("19.3M", ValueParser.formatCompact(19_300_000.0))
    }

    @Test
    fun testFormatCompact_B() {
        assertEquals("1.5B", ValueParser.formatCompact(1_500_000_000.0))
    }

    @Test
    fun testFormatCompact_K() {
        assertEquals("764.9K", ValueParser.formatCompact(764_900.0))
    }

    @Test
    fun testFormatCompact_small() {
        assertEquals("500", ValueParser.formatCompact(500.0))
    }

    @Test
    fun testFullFixture() {
        val text = "Food  19.3M  19.5M\nWood  20.4M  21.9M\nStone 5.8M   6.1M\nGold  1.1M   3.0M"
        val rows = ResourceTableParser.parse(text)
        val fromItems = rows.sumOf { it.fromItems }
        val total = rows.sumOf { it.total }
        assertEquals("46.6M", ValueParser.formatCompact(fromItems))
        assertEquals("50.5M", ValueParser.formatCompact(total))
    }
}
