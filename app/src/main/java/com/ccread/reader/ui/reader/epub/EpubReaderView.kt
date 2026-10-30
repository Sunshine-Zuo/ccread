package com.ccread.reader.ui.reader.epub

import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.JavascriptInterface
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import com.ccread.reader.data.epub.EpubSession
import com.ccread.reader.data.epub.EpubStyle
import com.ccread.reader.domain.model.Book
import com.ccread.reader.domain.model.PageMode
import com.ccread.reader.domain.model.ReadingSettings
import com.ccread.reader.ui.reader.ReaderInteraction
import com.ccread.reader.ui.reader.TapZone
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.input.DragEvent
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.input.KeyEvent as ReadiumKeyEvent
import org.readium.r2.navigator.input.TapEvent
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication

@Composable
fun EpubReaderView(
    book: Book,
    settings: ReadingSettings,
    mode: PageMode,
    initialLocatorJson: String?,
    jumpHref: String?,
    onJumpConsumed: () -> Unit,
    onTocLoaded: (List<com.ccread.reader.domain.model.TocNode>) -> Unit,
    onCenterTap: () -> Unit,
    onImageTap: (String) -> Unit,
    onLocatorChanged: (locatorJson: String, chapterTitle: String, percent: Double) -> Unit,
    onMissingFile: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current.density
    val fontScale = LocalDensity.current.fontScale
    val session = remember(book.uri) { EpubSession(context) }
    var publication by remember { mutableStateOf<Publication?>(null) }
    var fragment by remember { mutableStateOf<EpubNavigatorFragment?>(null) }
    val containerId = remember { View.generateViewId() }
    val currentOnImageTap by rememberUpdatedState(onImageTap)
    val imageBridge = remember { ImageBridge { currentOnImageTap(it) } }

    LaunchedEffect(book.uri) {
        if (session.open(android.net.Uri.parse(book.uri))) {
            publication = session.publication
            onTocLoaded(session.tocTree())
        } else {
            onMissingFile()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            fragment?.let { current ->
                runCatching {
                    activity?.supportFragmentManager
                        ?.beginTransaction()
                        ?.remove(current)
                        ?.commitNowAllowingStateLoss()
                }
            }
            session.close()
        }
    }

    val pub = publication
    if (pub == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("正在打开 EPUB…")
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx -> FragmentContainerView(ctx).apply { id = containerId } },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                if (fragment == null && activity != null) {
                    val initial = initialLocatorJson?.let { EpubSession.parseLocator(it) }
                    val factory = EpubNavigatorFactory(pub)
                        .createFragmentFactory(
                            initialLocator = initial,
                            initialPreferences = EpubStyle.preferences(settings, mode, fontScale),
                            configuration = EpubNavigatorFragment.Configuration(
                                shouldApplyInsetsPadding = false,
                            ).apply {
                                registerJavascriptInterface("CCReadBridge") { imageBridge }
                            },
                        )
                    val navigator = factory.instantiate(
                        EpubNavigatorFragment::class.java.classLoader,
                        EpubNavigatorFragment::class.java.name,
                    ) as EpubNavigatorFragment
                    activity.supportFragmentManager
                        .beginTransaction()
                        .replace(view.id, navigator, com.ccread.reader.MainActivity.EPUB_TAG)
                        .commitNowAllowingStateLoss()
                    fragment = navigator
                    navigator.view?.fitsSystemWindows = false
                }
            },
        )
    }

    LaunchedEffect(fragment, mode) {
        val navigator = fragment ?: return@LaunchedEffect
        val listener = object : InputListener {
            override fun onTap(event: TapEvent): Boolean {
                val navigatorWidth = navigator.view?.width ?: return false
                if (navigatorWidth <= 0) return false
                val cssX = event.point.x / density
                val cssY = event.point.y / density
                scope.launch {
                    val probe = "(()=>{var e=document.elementFromPoint($cssX,$cssY);" +
                        "if(!e)return 'none';" +
                        "if(e.tagName==='IMG'||e.tagName==='SVG'||e.closest('figure'))return 'image';" +
                        "return 'text';})()"
                    val target = runCatching { navigator.evaluateJavascript(probe) }
                        .getOrNull()
                        ?.lowercase()
                    // Images are opened by the page script through the CCReadBridge interface.
                    if (target != null && target.contains("image")) return@launch
                    when (ReaderInteraction.zoneFor(event.point.x, navigatorWidth.toFloat())) {
                        TapZone.LEFT -> if (mode == PageMode.PAGED) navigator.goBackward(false)
                        TapZone.RIGHT -> if (mode == PageMode.PAGED) navigator.goForward(false)
                        TapZone.CENTER -> onCenterTap()
                    }
                }
                return true
            }

            override fun onDrag(event: DragEvent): Boolean = false

            override fun onKey(event: ReadiumKeyEvent): Boolean = false
        }
        navigator.addInputListener(listener)
        try {
            awaitCancellation()
        } finally {
            navigator.removeInputListener(listener)
        }
    }

    LaunchedEffect(
        fragment,
        settings.fontSize,
        settings.backgroundId,
        settings.textColorArgb,
        settings.marginHorizontal,
        mode,
        fontScale,
    ) {
        fragment?.submitPreferences(EpubStyle.preferences(settings, mode, fontScale))
    }

    LaunchedEffect(fragment, publication) {
        val navigator = fragment ?: return@LaunchedEffect
        var lastHref: String? = null
        navigator.currentLocator.collect { locator ->
            val json = locator.toJSON().toString()
            val href = locator.href.toString()
            val title = EpubSession.titleIn(session.tocTree(), href) ?: locator.title.orEmpty()
            val percent = (locator.locations?.totalProgression ?: 0.0) * 100.0
            onLocatorChanged(json, title, percent)

            if (href != lastHref) {
                lastHref = href
                runCatching { navigator.evaluateJavascript(EPUB_IMAGE_SCRIPT) }
            }
        }
    }

    LaunchedEffect(jumpHref, fragment, publication) {
        val target = jumpHref ?: return@LaunchedEffect
        val navigator = fragment ?: return@LaunchedEffect
        val pub = publication ?: return@LaunchedEffect
        val allLinks = flatten(pub.tableOfContents)
        val link = allLinks.firstOrNull { it.href.toString() == target }
            ?: allLinks.firstOrNull {
                it.href.toString().substringBefore('#') == target.substringBefore('#')
            }
        if (link != null) navigator.go(link, false)
        onJumpConsumed()
    }
}

private fun flatten(links: List<Link>): List<Link> =
    links.flatMap { listOf(it) + flatten(it.children) }

/**
 * Registered on every resource by Readium, which guarantees it exists before the page script
 * runs. Images are sent as downscaled data URLs so the viewer can be a native full-screen window.
 */
private class ImageBridge(private val onImage: (String) -> Unit) {

    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun openImage(dataUrl: String) {
        if (dataUrl.startsWith("data:image")) {
            mainHandler.post { onImage(dataUrl) }
        }
    }
}

private const val EPUB_IMAGE_SCRIPT = """
(function () {
  var styleId = 'ccread-image-style';
  if (!document.getElementById(styleId)) {
    var style = document.createElement('style');
    style.id = styleId;
    style.textContent = 'img,svg{display:block !important;margin-left:auto !important;margin-right:auto !important;text-indent:0 !important;}figure{text-align:center !important;}';
    (document.head || document.documentElement).appendChild(style);
  }
  function open(img) {
    try {
      var w = img.naturalWidth, h = img.naturalHeight;
      if (!w || !h) return;
      var scale = Math.min(1, 2048 / Math.max(w, h));
      var canvas = document.createElement('canvas');
      canvas.width = Math.max(1, Math.round(w * scale));
      canvas.height = Math.max(1, Math.round(h * scale));
      canvas.getContext('2d').drawImage(img, 0, 0, canvas.width, canvas.height);
      var data = canvas.toDataURL('image/webp', 0.92);
      if (data.indexOf('data:image') !== 0) data = canvas.toDataURL('image/png');
      if (window.CCReadBridge) window.CCReadBridge.openImage(data);
    } catch (e) {
    }
  }
  var images = document.querySelectorAll('img');
  for (var i = 0; i < images.length; i++) {
    (function (img) {
      if (img.dataset.ccreadTap) return;
      img.dataset.ccreadTap = '1';
      img.addEventListener('click', function (event) {
        event.preventDefault();
        event.stopPropagation();
        open(img);
      }, true);
    })(images[i]);
  }
})();
"""
