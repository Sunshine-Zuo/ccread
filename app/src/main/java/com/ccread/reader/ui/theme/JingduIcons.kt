package com.ccread.reader.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** 统一的 1.8dp 描边线性图标，避免 Material 默认图标的填充感。 */
object JingduIcons {

    val Plus: ImageVector by lazy {
        lineIcon("Plus") {
            moveTo(12f, 5f); lineTo(12f, 19f)
            moveTo(5f, 12f); lineTo(19f, 12f)
        }
    }

    val Close: ImageVector by lazy {
        lineIcon("Close") {
            moveTo(6f, 6f); lineTo(18f, 18f)
            moveTo(18f, 6f); lineTo(6f, 18f)
        }
    }

    val ChevronLeft: ImageVector by lazy {
        lineIcon("ChevronLeft") {
            moveTo(15f, 5f); lineTo(8f, 12f); lineTo(15f, 19f)
        }
    }

    val ChevronRight: ImageVector by lazy {
        lineIcon("ChevronRight") {
            moveTo(9f, 5f); lineTo(16f, 12f); lineTo(9f, 19f)
        }
    }

    val ChevronDown: ImageVector by lazy {
        lineIcon("ChevronDown") {
            moveTo(5f, 9f); lineTo(12f, 16f); lineTo(19f, 9f)
        }
    }

    val List: ImageVector by lazy {
        lineIcon("List") {
            moveTo(4f, 7f); lineTo(20f, 7f)
            moveTo(4f, 12f); lineTo(20f, 12f)
            moveTo(4f, 17f); lineTo(20f, 17f)
        }
    }

    val Sun: ImageVector by lazy {
        lineIcon("Sun") {
            moveTo(12f, 7.8f)
            curveTo(14.32f, 7.8f, 16.2f, 9.68f, 16.2f, 12f)
            curveTo(16.2f, 14.32f, 14.32f, 16.2f, 12f, 16.2f)
            curveTo(9.68f, 16.2f, 7.8f, 14.32f, 7.8f, 12f)
            curveTo(7.8f, 9.68f, 9.68f, 7.8f, 12f, 7.8f)
            close()
            moveTo(12f, 2.5f); lineTo(12f, 4.6f)
            moveTo(12f, 19.4f); lineTo(12f, 21.5f)
            moveTo(2.5f, 12f); lineTo(4.6f, 12f)
            moveTo(19.4f, 12f); lineTo(21.5f, 12f)
            moveTo(5.3f, 5.3f); lineTo(6.8f, 6.8f)
            moveTo(17.2f, 17.2f); lineTo(18.7f, 18.7f)
            moveTo(18.7f, 5.3f); lineTo(17.2f, 6.8f)
            moveTo(6.8f, 17.2f); lineTo(5.3f, 18.7f)
        }
    }

    val Check: ImageVector by lazy {
        lineIcon("Check") {
            moveTo(5f, 12.5f); lineTo(9.5f, 17f); lineTo(19f, 7.5f)
        }
    }

    val Book: ImageVector by lazy {
        lineIcon("Book") {
            moveTo(4f, 6.5f)
            curveTo(7f, 5f, 10f, 5.2f, 12f, 7f)
            curveTo(14f, 5.2f, 17f, 5f, 20f, 6.5f)
            lineTo(20f, 18.5f)
            curveTo(17f, 17f, 14f, 17.2f, 12f, 19f)
            curveTo(10f, 17.2f, 7f, 17f, 4f, 18.5f)
            close()
            moveTo(12f, 7f); lineTo(12f, 19f)
        }
    }
}

private fun lineIcon(name: String, pathBuilder: PathBuilder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = pathBuilder,
        )
    }.build()
