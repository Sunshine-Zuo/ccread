package com.ccread.reader.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
sealed interface ReadingProgress {
    val percent: Double
}

@Serializable
@SerialName("txt")
data class TxtProgress(
    override val percent: Double,
    val chapterIndex: Int,
    val charOffsetInChapter: Long,
) : ReadingProgress

@Serializable
@SerialName("epub")
data class EpubProgress(
    override val percent: Double,
    val locatorJson: String,
) : ReadingProgress

object ProgressCodec {
    private val json = Json { ignoreUnknownKeys = true }

    fun encode(progress: ReadingProgress): String =
        json.encodeToString(ReadingProgress.serializer(), progress)

    fun decode(raw: String?): ReadingProgress? {
        if (raw.isNullOrBlank()) return null
        return runCatching { json.decodeFromString(ReadingProgress.serializer(), raw) }.getOrNull()
    }
}
