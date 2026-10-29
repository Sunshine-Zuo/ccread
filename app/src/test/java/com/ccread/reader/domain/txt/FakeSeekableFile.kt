package com.ccread.reader.domain.txt

import java.io.ByteArrayInputStream
import java.io.InputStream

class FakeSeekableFile(private val bytes: ByteArray) : SeekableFile {

    var maxReadLength: Int = 0
        private set

    override val sizeBytes: Long get() = bytes.size.toLong()

    override fun readAt(offset: Long, length: Int): ByteArray {
        maxReadLength = maxOf(maxReadLength, length)
        val start = offset.toInt().coerceIn(0, bytes.size)
        val end = (start + length).coerceIn(start, bytes.size)
        return bytes.copyOfRange(start, end)
    }

    override fun openStreamAt(offset: Long): InputStream =
        ByteArrayInputStream(readAt(offset, (sizeBytes - offset).toInt()))

    override fun close() = Unit
}
