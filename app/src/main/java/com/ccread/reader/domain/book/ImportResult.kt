package com.ccread.reader.domain.book

sealed interface ImportResult {
    data class Success(val bookId: Long) : ImportResult
    data class TooLarge(val limitBytes: Long) : ImportResult
    data object UnsupportedFormat : ImportResult
    data object NotSeekable : ImportResult
    data object Duplicate : ImportResult
    data class Failed(val message: String) : ImportResult
}
