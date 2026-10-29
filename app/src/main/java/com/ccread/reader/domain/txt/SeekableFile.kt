package com.ccread.reader.domain.txt

import java.io.InputStream

interface SeekableFile {
    val sizeBytes: Long
    fun readAt(offset: Long, length: Int): ByteArray
    fun openStreamAt(offset: Long): InputStream
    fun close()
}
