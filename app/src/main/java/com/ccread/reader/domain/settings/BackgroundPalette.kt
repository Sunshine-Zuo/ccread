package com.ccread.reader.domain.settings

import androidx.compose.ui.graphics.Color
import com.ccread.reader.domain.model.BackgroundId

data class BackgroundStyle(
    val background: Color,
    val text: Color,
)

object BackgroundPalette {

    val all: List<BackgroundId> = BackgroundId.entries

    fun styleOf(id: BackgroundId): BackgroundStyle = when (id) {
        BackgroundId.WHITE -> BackgroundStyle(Color(0xFFFFFFFF), Color(0xFF1A1A1A))
        BackgroundId.BEIGE -> BackgroundStyle(Color(0xFFF5ECD8), Color(0xFF3B2F2F))
        BackgroundId.GREEN -> BackgroundStyle(Color(0xFFCCE8CF), Color(0xFF1F2A1F))
        BackgroundId.DARK_GRAY -> BackgroundStyle(Color(0xFF303030), Color(0xFFD0D0D0))
        BackgroundId.BLACK -> BackgroundStyle(Color(0xFF000000), Color(0xFFE8E8E8))
    }
}

object BrightnessController {
    fun windowBrightness(followSystem: Boolean, value: Float): Float =
        if (followSystem) -1f else value.coerceIn(0f, 1f)
}
