package com.hermes.wearos.core.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextUtilTest {

    @Test
    fun testTruncateToWearLimit_shortText() {
        val input = "BTC masih bullish. Support utama 118k."
        val result = WearTextUtils.truncateToWearLimit(input, 50)
        assertEquals(input, result)
    }

    @Test
    fun testTruncateToWearLimit_longText() {
        val words = (1..60).map { "kata$it" }.joinToString(" ")
        val result = WearTextUtils.truncateToWearLimit(words, 50)
        assertTrue(result.endsWith("..."))
        val resultWords = result.removeSuffix("...").trim().split(" ")
        assertEquals(50, resultWords.size)
    }

    @Test
    fun testFormatServerStatus() {
        val result = WearTextUtils.formatServerStatus(21.4f, 58.7f)
        assertEquals("CPU 21% • RAM 59%", result)
    }

    @Test
    fun testFormatRelativeTime_justNow() {
        val now = System.currentTimeMillis()
        val result = DateUtils.formatRelativeTime(now - 10_000)
        assertEquals("Baru saja", result)
    }

    @Test
    fun testFormatRelativeTime_minutesAgo() {
        val now = System.currentTimeMillis()
        val result = DateUtils.formatRelativeTime(now - 5 * 60_000)
        assertEquals("5m lalu", result)
    }

    @Test
    fun testFormatRelativeTime_hoursAgo() {
        val now = System.currentTimeMillis()
        val result = DateUtils.formatRelativeTime(now - 3 * 3_600_000)
        assertEquals("3j lalu", result)
    }
}
