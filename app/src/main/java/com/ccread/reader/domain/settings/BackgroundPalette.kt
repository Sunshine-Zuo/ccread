package com.ccread.reader.domain.settings

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.ccread.reader.domain.model.BackgroundId

data class BackgroundStyle(
    val background: Color,
    val text: Color,
)

data class BackgroundPreset(
    val id: BackgroundId,
    val background: Int,
    val text: Int,
)

object BackgroundPalette {

    /**
     * 背景与默认文字颜色一一对应：每个背景预设都带一个配套的字色，
     * 字体颜色的预置列表直接取这些字色，数量与顺序完全一致。
     */
    val presets: List<BackgroundPreset> = listOf(
        BackgroundPreset(BackgroundId.WHITE, 0xFFFFFFFF.toInt(), 0xFF1A1A1A.toInt()),
        BackgroundPreset(BackgroundId.BEIGE, 0xFFF5ECD8.toInt(), 0xFF3B2F2F.toInt()),
        BackgroundPreset(BackgroundId.GREEN, 0xFFCCE8CF.toInt(), 0xFF1F2A1F.toInt()),
        BackgroundPreset(BackgroundId.DARK_GRAY, 0xFF303030.toInt(), 0xFFD0D0D0.toInt()),
        BackgroundPreset(BackgroundId.BLACK, 0xFF000000.toInt(), 0xFFE8E8E8.toInt()),
    )

    val all: List<BackgroundId> = presets.map { it.id }

    fun styleOf(id: BackgroundId): BackgroundStyle {
        val preset = presets.firstOrNull { it.id == id } ?: presets.first()
        return BackgroundStyle(Color(preset.background), Color(preset.text))
    }

    fun resolve(backgroundId: BackgroundId, customArgb: Int?): BackgroundStyle {
        if (customArgb == null) return styleOf(backgroundId)
        val background = Color(customArgb)
        return BackgroundStyle(background, autoTextColor(background))
    }

    fun autoTextColor(background: Color): Color {
        val dark = Color(0xFF1A1A1A)
        val light = Color(0xFFE8E8E8)
        val backgroundLuminance = background.luminance()
        val darkContrast = (backgroundLuminance + 0.05f) / (dark.luminance() + 0.05f)
        val lightContrast = (light.luminance() + 0.05f) / (backgroundLuminance + 0.05f)
        return if (darkContrast >= lightContrast) dark else light
    }
}

object BrightnessController {
    fun windowBrightness(followSystem: Boolean, value: Float): Float =
        if (followSystem) -1f else value.coerceIn(0f, 1f)
}
