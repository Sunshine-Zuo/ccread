package com.ccread.reader.ui.reader

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.View
import android.view.ViewParent
import android.view.Window
import android.view.WindowManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ImageData {

    fun decode(dataUrl: String): ByteArray? {
        val comma = dataUrl.indexOf(',')
        if (comma < 0) return null
        return runCatching {
            Base64.getDecoder().decode(dataUrl.substring(comma + 1))
        }.getOrNull()
    }
}

@Composable
fun ImageViewerDialog(
    dataUrl: String?,
    visible: Boolean,
    onDismiss: () -> Unit,
) {
    if (!visible || dataUrl == null) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        val view = LocalView.current
        DisposableEffect(view) {
            val window = view.findDialogWindow()
            if (window != null) {
                window.setDimAmount(0.25f)
                window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                WindowCompat.setDecorFitsSystemWindows(window, false)
                window.setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                )
                WindowInsetsControllerCompat(window, view).hide(WindowInsetsCompat.Type.systemBars())
            }
            onDispose { }
        }

        val bitmap by produceState<Bitmap?>(null, dataUrl) {
            value = withContext(Dispatchers.IO) {
                val bytes = ImageData.decode(dataUrl) ?: return@withContext null
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
        }

        var scale by remember { mutableFloatStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        var size by remember { mutableStateOf(IntSize.Zero) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { size = it }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val next = (scale * zoom).coerceIn(1f, 5f)
                        scale = next
                        offset = if (next <= 1f) {
                            Offset.Zero
                        } else {
                            val maxX = size.width * (next - 1f) / 2f
                            val maxY = size.height * (next - 1f) / 2f
                            Offset(
                                (offset.x + pan.x).coerceIn(-maxX, maxX),
                                (offset.y + pan.y).coerceIn(-maxY, maxY),
                            )
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onDismiss() })
                },
            contentAlignment = Alignment.Center,
        ) {
            bitmap?.let { image ->
                Image(
                    bitmap = image.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y,
                        ),
                    contentScale = ContentScale.Fit,
                    filterQuality = FilterQuality.Medium,
                )
            }
        }
    }
}

private fun View.findDialogWindow(): Window? {
    var parent: ViewParent? = this.parent
    while (parent != null) {
        if (parent is DialogWindowProvider) return parent.window
        parent = parent.parent
    }
    return null
}
