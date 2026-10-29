package com.ccread.reader.domain.txt

import org.junit.Assert.assertEquals
import org.junit.Test

class CharsetDetectorTest {

    @Test
    fun `UTF-8 BOM is detected`() {
        val bytes = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + "a".toByteArray()
        assertEquals("UTF-8", CharsetDetector.detect(bytes).name())
    }

    @Test
    fun `UTF-16LE BOM is detected`() {
        val bytes = byteArrayOf(0xFF.toByte(), 0xFE.toByte(), 0x41, 0x00)
        assertEquals("UTF-16LE", CharsetDetector.detect(bytes).name())
    }

    @Test
    fun `UTF-16BE BOM is detected`() {
        val bytes = byteArrayOf(0xFE.toByte(), 0xFF.toByte(), 0x00, 0x41)
        assertEquals("UTF-16BE", CharsetDetector.detect(bytes).name())
    }

    @Test
    fun `GBK without BOM falls back to GB18030`() {
        val bytes = "第一章 起风了".toByteArray(charset("GBK"))
        assertEquals("GB18030", CharsetDetector.detect(bytes).name())
    }

    @Test
    fun `random bytes do not throw`() {
        CharsetDetector.detect(ByteArray(4096) { it.toByte() })
    }
}
