package com.ccread.reader.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ccread.reader.domain.model.BookFormat
import com.ccread.reader.domain.model.PageMode
import com.ccread.reader.domain.model.ReadingSettings
import com.ccread.reader.domain.settings.BackgroundPalette
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingSettingsSheet(
    settings: ReadingSettings,
    format: BookFormat,
    onChange: (ReadingSettings) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
        ) {
            SettingSlider(
                label = "字号 ${settings.fontSize}",
                value = settings.fontSize.toFloat(),
                range = 12f..36f,
                onChange = { onChange(settings.copy(fontSize = it.roundToInt())) },
            )
            SettingSlider(
                label = "左右边距 ${settings.marginHorizontal}",
                value = settings.marginHorizontal.toFloat(),
                range = 8f..48f,
                enabled = format != BookFormat.EPUB,
                onChange = { onChange(settings.copy(marginHorizontal = it.roundToInt())) },
            )
            SettingSlider(
                label = "行距 ${"%.1f".format(settings.lineSpacing)}",
                value = settings.lineSpacing,
                range = 1.0f..2.0f,
                enabled = format != BookFormat.EPUB,
                onChange = { onChange(settings.copy(lineSpacing = it)) },
            )

            Text("背景", modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BackgroundPalette.all.forEach { id ->
                    val style = BackgroundPalette.styleOf(id)
                    val selected = settings.backgroundId == id
                    Spacer(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(style.background)
                            .border(
                                width = if (selected) 3.dp else 1.dp,
                                color = if (selected) style.text else style.text.copy(alpha = 0.3f),
                                shape = CircleShape,
                            )
                            .clickable { onChange(settings.copy(backgroundId = id)) },
                    )
                }
            }

            Text("翻页模式", modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilterChip(
                    selected = settings.pageMode == PageMode.PAGED,
                    onClick = { onChange(settings.copy(pageMode = PageMode.PAGED)) },
                    label = { Text("左右翻页") },
                )
                FilterChip(
                    selected = settings.pageMode == PageMode.SCROLL,
                    onClick = { onChange(settings.copy(pageMode = PageMode.SCROLL)) },
                    label = { Text("上下滚动") },
                )
            }
        }
    }
}

@Composable
private fun SettingSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    enabled: Boolean = true,
    onChange: (Float) -> Unit,
) {
    var local by remember(value) { mutableFloatStateOf(value) }
    Text(label, modifier = Modifier.padding(top = 12.dp))
    Slider(
        value = local,
        onValueChange = { local = it },
        onValueChangeFinished = { onChange(local) },
        valueRange = range,
        enabled = enabled,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrightnessSheet(
    settings: ReadingSettings,
    onChange: (ReadingSettings) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("跟随系统亮度")
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = settings.followSystemBrightness,
                    onCheckedChange = { onChange(settings.copy(followSystemBrightness = it)) },
                )
            }
            Slider(
                value = settings.brightness,
                onValueChange = { onChange(settings.copy(brightness = it)) },
                valueRange = 0f..1f,
                enabled = !settings.followSystemBrightness,
            )
        }
    }
}
