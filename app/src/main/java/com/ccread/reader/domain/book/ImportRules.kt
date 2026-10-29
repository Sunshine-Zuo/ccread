package com.ccread.reader.domain.book

import com.ccread.reader.domain.model.BookFormat
import java.util.Locale

object ImportRules {

    fun formatOf(fileName: String, mime: String?): BookFormat? {
        val extension = fileName.substringAfterLast('.', "").lowercase(Locale.US)
        return when {
            extension == "txt" -> BookFormat.TXT
            extension == "epub" -> BookFormat.EPUB
            mime == "text/plain" -> BookFormat.TXT
            mime == "application/epub+zip" -> BookFormat.EPUB
            else -> null
        }
    }

    fun limitBytes(format: BookFormat): Long = when (format) {
        BookFormat.TXT -> 200L * 1024 * 1024
        BookFormat.EPUB -> 500L * 1024 * 1024
    }

    fun titleOf(fileName: String): String {
        val dot = fileName.lastIndexOf('.')
        return if (dot < 0) fileName else fileName.substring(0, dot)
    }
}
