package com.ccread.reader.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val format: String,
    val uri: String,
    val sizeBytes: Long,
    val importedAt: Long,
    val lastReadAt: Long?,
    val progressJson: String?,
    val encodingName: String?,
    val totalChars: Long?,
)
