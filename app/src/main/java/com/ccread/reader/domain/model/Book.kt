package com.ccread.reader.domain.model

enum class BookFormat { TXT, EPUB }

data class Book(
    val id: Long,
    val title: String,
    val format: BookFormat,
    val uri: String,
    val sizeBytes: Long,
    val importedAt: Long,
    val lastReadAt: Long?,
    val progress: ReadingProgress?,
    val encodingName: String?,
    val totalChars: Long?,
)
