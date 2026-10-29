package com.ccread.reader.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "chapters",
    primaryKeys = ["bookId", "chapterIndex"],
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("bookId")],
)
data class ChapterEntity(
    val bookId: Long,
    val chapterIndex: Int,
    val title: String,
    val startByte: Long,
    val endByte: Long,
    val charStart: Long,
    val charCount: Long,
)
