package com.ccread.reader.domain.model

data class Chapter(
    val bookId: Long,
    val index: Int,
    val title: String,
    val startByte: Long,
    val endByte: Long,
    val charStart: Long,
    val charCount: Long,
)

data class TocNode(
    val title: String,
    val href: String?,
    val children: List<TocNode> = emptyList(),
)
