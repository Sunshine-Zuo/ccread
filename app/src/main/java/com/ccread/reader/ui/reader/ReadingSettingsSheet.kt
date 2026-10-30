package com.ccread.reader.ui.reader

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ccread.reader.domain.model.BookFormat
import com.ccread.reader.domain.model.PageMode
import com.ccread.reader.domain.model.ReadingSettings
import com.ccread.reader.domain.settings.BackgroundPalette
import com.ccread.reader.domain.settings.FontColorPalette
import com.ccread.reader.ui.theme.JingduIcons
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingSettingsSheet(
    settings: ReadingSettings,
    format: BookFormat,
    onChange: (ReadingSettings) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var colorPickerTarget by remember { mutableStateOf<ColorPickerTarget?>(null) }
    val resolvedBackground = BackgroundPalette.resolve(
        settings.backgroundId,
        settings.customBackgroundArgb,
    )
    val effectiveTextColor = settings.textColorArgb
        ?.let { Color(it) }
        ?: resolvedBackground.text

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = Color.Black.copy(alpha = 0.32f),
    ) {
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 32.dp) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 20.dp),
            ) {
                Text(
                    text = "阅读设置",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 2.dp),
                )

                CompactSlider(
                    label = "字号",
                    valueText = settings.fontSize.toString(),
                    value = settings.fontSize.toFloat(),
                    range = 12f..36f,
                    onChange = { onChange(settings.copy(fontSize = it.roundToInt())) },
                )
                CompactSlider(
                    label = "左右边距",
                    valueText = settings.marginHorizontal.toString(),
                    value = settings.marginHorizontal.toFloat(),
                    range = 8f..48f,
                    enabled = format != BookFormat.EPUB,
                    onChange = { onChange(settings.copy(marginHorizontal = it.roundToInt())) },
                )
                CompactSlider(
                    label = "行距",
                    valueText = "%.1f".format(settings.lineSpacing),
                    value = settings.lineSpacing,
                    range = 1.0f..2.0f,
                    enabled = format != BookFormat.EPUB,
                    onChange = { onChange(settings.copy(lineSpacing = it)) },
                )

                SectionDivider()

                SectionHeader("字体颜色") {
                    TextButton(
                        onClick = { onChange(settings.copy(textColorArgb = null)) },
                        enabled = settings.textColorArgb != null,
                        contentPadding = PaddingValues(horizontal = 8.dp),
                    ) {
                        Text(
                            text = "跟随背景",
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FontColorPalette.presets.forEach { argb ->
                        ColorSwatch(
                            color = Color(argb),
                            selected = settings.textColorArgb == argb ||
                                (
                                    settings.textColorArgb == null &&
                                        resolvedBackground.text == Color(argb)
                                    ),
                            onClick = { onChange(settings.copy(textColorArgb = argb)) },
                        )
                    }
                    CustomColorSwatch(
                        selectedColor = settings.textColorArgb
                            ?.takeIf { it !in FontColorPalette.presets }
                            ?.let { Color(it) },
                        onClick = { colorPickerTarget = ColorPickerTarget.FONT },
                    )
                }

                SectionDivider()

                SectionHeader("背景")
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    BackgroundPalette.presets.forEach { preset ->
                        ColorSwatch(
                            color = Color(preset.background),
                            selected = settings.customBackgroundArgb == null &&
                                settings.backgroundId == preset.id,
                            onClick = {
                                onChange(
                                    settings.copy(
                                        backgroundId = preset.id,
                                        customBackgroundArgb = null,
                                        textColorArgb = null,
                                    ),
                                )
                            },
                        )
                    }
                    CustomColorSwatch(
                        selectedColor = settings.customBackgroundArgb?.let { Color(it) },
                        onClick = { colorPickerTarget = ColorPickerTarget.BACKGROUND },
                    )
                }

                SectionDivider()

                SectionHeader("翻页模式")
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val options = listOf(
                        PageMode.PAGED to "左右翻页",
                        PageMode.SCROLL to "上下滚动",
                    )
                    options.forEachIndexed { index, (mode, label) ->
                        val selected = settings.pageMode == mode
                        SegmentedButton(
                            selected = selected,
                            onClick = { onChange(settings.copy(pageMode = mode)) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = options.size,
                                baseShape = RoundedCornerShape(6.dp),
                            ),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                inactiveContainerColor = MaterialTheme.colorScheme.surface,
                                inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                            border = BorderStroke(
                                width = if (selected) 1.dp else 0.7.dp,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                },
                            ),
                            label = {
                                Text(label, style = MaterialTheme.typography.titleSmall)
                            },
                        )
                    }
                }
            }
        }
    }

    val target = colorPickerTarget
    if (target != null) {
        val initial = when (target) {
            ColorPickerTarget.FONT -> effectiveTextColor
            ColorPickerTarget.BACKGROUND -> settings.customBackgroundArgb
                ?.let { Color(it) }
                ?: BackgroundPalette.styleOf(settings.backgroundId).background
        }
        ColorPickerDialog(
            title = when (target) {
                ColorPickerTarget.FONT -> "自定义字体颜色"
                ColorPickerTarget.BACKGROUND -> "自定义背景颜色"
            },
            initial = initial,
            onDismiss = { colorPickerTarget = null },
            onConfirm = { color ->
                when (target) {
                    ColorPickerTarget.FONT -> onChange(
                        settings.copy(textColorArgb = color.toArgb()),
                    )

                    ColorPickerTarget.BACKGROUND -> onChange(
                        settings.copy(
                            customBackgroundArgb = color.toArgb(),
                            textColorArgb = null,
                        ),
                    )
                }
                colorPickerTarget = null
            },
        )
    }
}

private enum class ColorPickerTarget { FONT, BACKGROUND }

@Composable
private fun SectionHeader(label: String, trailing: (@Composable () -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp),
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (trailing != null) {
            Spacer(modifier = Modifier.weight(1f))
            trailing()
        }
    }
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 12.dp),
        thickness = 0.7.dp,
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactSlider(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    enabled: Boolean = true,
    liveUpdate: Boolean = false,
    onChange: (Float) -> Unit,
) {
    var local by remember(value) { mutableFloatStateOf(value) }
    val activeColor = if (enabled) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)
    }
    val trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)

    Column(modifier = Modifier.padding(top = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFeatureSettings = "tnum",
                    letterSpacing = 0.sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Slider(
            value = local,
            onValueChange = {
                local = it
                if (liveUpdate) onChange(it)
            },
            onValueChangeFinished = { if (!liveUpdate) onChange(local) },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            thumb = {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(MaterialTheme.colorScheme.surface, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(activeColor, CircleShape),
                    )
                }
            },
            track = {
                val fraction = if (range.endInclusive > range.start) {
                    ((local - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)
                } else {
                    0f
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(trackColor),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .fillMaxHeight()
                            .background(activeColor),
                    )
                }
            },
            valueRange = range,
        )
    }
}

@Composable
private fun ColorSwatch(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val checkTint = if (color.luminance() > 0.5f) {
        Color.Black.copy(alpha = 0.62f)
    } else {
        Color.White.copy(alpha = 0.92f)
    }
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(4.dp))
            .border(
                width = if (selected) 1.5.dp else 0.dp,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    Color.Transparent
                },
                shape = RoundedCornerShape(4.dp),
            )
            .clickable(onClick = onClick)
            .padding(4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(3.dp))
                .background(color)
                .border(
                    width = 0.7.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(3.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(
                    imageVector = JingduIcons.Check,
                    contentDescription = null,
                    tint = checkTint,
                    modifier = Modifier.size(13.dp),
                )
            }
        }
    }
}

@Composable
private fun CustomColorSwatch(selectedColor: Color?, onClick: () -> Unit) {
    val selected = selectedColor != null
    val ink = MaterialTheme.colorScheme.onSurface
    val paper = MaterialTheme.colorScheme.surface
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(4.dp))
            .border(
                width = if (selected) 1.5.dp else 0.dp,
                color = if (selected) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    Color.Transparent
                },
                shape = RoundedCornerShape(4.dp),
            )
            .clickable(onClick = onClick)
            .padding(4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val paletteBody = Path().apply {
                moveTo(width * 0.10f, height * 0.52f)
                cubicTo(width * 0.08f, height * 0.22f, width * 0.32f, height * 0.10f, width * 0.56f, height * 0.15f)
                cubicTo(width * 0.86f, height * 0.21f, width * 0.96f, height * 0.48f, width * 0.82f, height * 0.72f)
                cubicTo(width * 0.64f, height * 0.96f, width * 0.28f, height * 0.92f, width * 0.13f, height * 0.72f)
                close()
            }

            if (selectedColor == null) {
                // 未选择：整个调色盘铺满彩色
                drawPath(
                    path = paletteBody,
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color(0xFFC0392B),
                            Color(0xFFD9A441),
                            Color(0xFF4F8A5B),
                            Color(0xFF3E7CA6),
                            Color(0xFF6B5AA6),
                            Color(0xFFB05A8E),
                            Color(0xFFC0392B),
                        ),
                        center = Offset(width * 0.5f, height * 0.5f),
                    ),
                )
            } else {
                // 已选择自定义色：盘身就是该颜色，保留颜料点
                drawPath(paletteBody, selectedColor)
                val dabs = listOf(
                    Color(0xFFC0392B),
                    Color(0xFFD9A441),
                    Color(0xFF4F8A5B),
                    Color(0xFF3E7CA6),
                    Color(0xFF6B5AA6),
                )
                dabs.forEachIndexed { index, color ->
                    drawCircle(
                        color = color,
                        radius = width * 0.06f,
                        center = Offset(width * (0.22f + index * 0.12f), height * 0.32f),
                    )
                }
            }

            drawPath(
                path = paletteBody,
                color = ink.copy(alpha = 0.35f),
                style = Stroke(width = 0.7.dp.toPx()),
            )
            val holeCenter = Offset(width * 0.76f, height * 0.60f)
            val holeRadius = width * 0.13f
            drawCircle(paper, holeRadius, holeCenter)
            drawCircle(
                color = ink.copy(alpha = 0.25f),
                radius = holeRadius,
                center = holeCenter,
                style = Stroke(width = 0.5.dp.toPx()),
            )
            if (selected) {
                val check = Path().apply {
                    moveTo(holeCenter.x - holeRadius * 0.4f, holeCenter.y)
                    lineTo(holeCenter.x - holeRadius * 0.05f, holeCenter.y + holeRadius * 0.35f)
                    lineTo(holeCenter.x + holeRadius * 0.5f, holeCenter.y - holeRadius * 0.35f)
                }
                drawPath(
                    path = check,
                    color = ink,
                    style = Stroke(
                        width = 0.9.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                        join = androidx.compose.ui.graphics.StrokeJoin.Round,
                    ),
                )
            }
        }
    }
}

@Composable
internal fun ColorPickerDialog(
    title: String,
    initial: Color,
    onDismiss: () -> Unit,
    onConfirm: (Color) -> Unit,
) {
    val initialHsv = remember(initial) {
        FloatArray(3).also { AndroidColor.colorToHSV(initial.toArgb(), it) }
    }
    var hue by remember(initial) { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember(initial) { mutableFloatStateOf(initialHsv[1]) }
    var brightness by remember(initial) { mutableFloatStateOf(initialHsv[2]) }
    val selected = Color(AndroidColor.HSVToColor(floatArrayOf(hue, saturation, brightness)))

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(12.dp),
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 28.dp) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ColorDisc(
                        hue = hue,
                        saturation = saturation,
                        brightness = brightness,
                        onPick = { h, s ->
                            hue = h
                            saturation = s
                        },
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .aspectRatio(1f),
                    )
                    Text(
                        text = "在圆盘上拖动选择颜色",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    CompactSlider(
                        label = "明度",
                        valueText = "${(brightness * 100).roundToInt()}%",
                        value = brightness,
                        range = 0f..1f,
                        liveUpdate = true,
                        onChange = { brightness = it },
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(selected)
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(3.dp),
                                ),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "#%06X".format(0xFFFFFF and selected.toArgb()),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFeatureSettings = "tnum",
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected) }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@Composable
private fun ColorDisc(
    hue: Float,
    saturation: Float,
    brightness: Float,
    onPick: (Float, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val outline = MaterialTheme.colorScheme.outlineVariant
    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { position ->
                    pickDiscColor(position, size, onPick)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { position ->
                        pickDiscColor(position, size, onPick)
                    },
                    onDrag = { change, _ ->
                        pickDiscColor(change.position, size, onPick)
                        change.consume()
                    },
                )
            },
    ) {
        val radius = size.minDimension / 2f
        val discCenter = center
        drawCircle(
            brush = Brush.sweepGradient(
                colors = listOf(
                    Color(0xFFFF0000),
                    Color(0xFFFFFF00),
                    Color(0xFF00FF00),
                    Color(0xFF00FFFF),
                    Color(0xFF0000FF),
                    Color(0xFFFF00FF),
                    Color(0xFFFF0000),
                ),
                center = discCenter,
            ),
            radius = radius,
            center = discCenter,
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, Color.White.copy(alpha = 0f)),
                center = discCenter,
                radius = radius,
            ),
            radius = radius,
            center = discCenter,
        )
        if (brightness < 1f) {
            drawCircle(
                color = Color.Black.copy(alpha = 1f - brightness),
                radius = radius,
                center = discCenter,
            )
        }

        val radians = Math.toRadians(hue.toDouble())
        val marker = Offset(
            x = discCenter.x + cos(radians).toFloat() * saturation * radius,
            y = discCenter.y + sin(radians).toFloat() * saturation * radius,
        )
        drawCircle(
            color = Color.White,
            radius = 8.dp.toPx(),
            center = marker,
            style = Stroke(width = 2.5.dp.toPx()),
        )
        drawCircle(
            color = Color.Black.copy(alpha = 0.4f),
            radius = 10.dp.toPx(),
            center = marker,
            style = Stroke(width = 1.dp.toPx()),
        )
        drawCircle(
            color = outline,
            radius = radius - 0.5.dp.toPx(),
            center = discCenter,
            style = Stroke(width = 1.dp.toPx()),
        )
    }
}

private fun pickDiscColor(position: Offset, size: IntSize, onPick: (Float, Float) -> Unit) {
    val centerX = size.width / 2f
    val centerY = size.height / 2f
    val radius = min(size.width, size.height) / 2f
    if (radius <= 0f) return
    val dx = position.x - centerX
    val dy = position.y - centerY
    val distance = sqrt(dx * dx + dy * dy)
    val saturation = (distance / radius).coerceIn(0f, 1f)
    var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
    if (angle < 0f) angle += 360f
    onPick(angle, saturation)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrightnessSheet(
    settings: ReadingSettings,
    onChange: (ReadingSettings) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = Color.Black.copy(alpha = 0.32f),
    ) {
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 32.dp) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 20.dp),
            ) {
                Text(
                    text = "亮度",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "跟随系统亮度",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = settings.followSystemBrightness,
                        onCheckedChange = {
                            onChange(settings.copy(followSystemBrightness = it))
                        },
                    )
                }
                CompactSlider(
                    label = "亮度",
                    valueText = "${(settings.brightness * 100).roundToInt()}%",
                    value = settings.brightness,
                    range = 0f..1f,
                    enabled = !settings.followSystemBrightness,
                    onChange = { onChange(settings.copy(brightness = it)) },
                )
            }
        }
    }
}
