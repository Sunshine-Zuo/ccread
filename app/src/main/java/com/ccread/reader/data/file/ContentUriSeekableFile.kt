package com.ccread.reader.data.file

import android.content.Context
import android.net.Uri
import com.ccread.reader.domain.txt.SeekableFile
import java.io.ByteArrayInputStream
import java.io.FileInputStream
import java.io.InputStream
import java.nio.ByteBuffer

class ContentUriSeekableFile(context: Context, uri: Uri) : SeekableFile {

    private val descriptor = context.contentResolver.openFileDescriptor(uri, "r")
        ?: error("无法打开文件")
    private val channel = FileInputStream(descriptor.fileDescriptor).channel

    override val sizeBytes: Long = descriptor.statSize.takeIf { it >= 0 } ?: channel.size()

    override fun readAt(offset: Long, length: Int): ByteArray {
        if (length <= 0) return ByteArray(0)
        val effective = minOf(length.toLong(), (sizeBytes - offset).coerceAtLeast(0)).toInt()
        if (effective <= 0) return ByteArray(0)
        val buffer = ByteBuffer.allocate(effective)
        var position = offset
        while (buffer.hasRemaining()) {
            val read = channel.read(buffer, position)
            if (read < 0) break
            position += read
        }
        return buffer.array().copyOf(buffer.position())
    }

    override fun openStreamAt(offset: Long): InputStream =
        ByteArrayInputStream(readAt(offset, (sizeBytes - offset).coerceAtLeast(0).toInt()))

    override fun close() {
        runCatching { channel.close() }
        runCatching { descriptor.close() }
    }
}
