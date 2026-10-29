package com.ccread.reader

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.FragmentActivity
import com.ccread.reader.ui.navigation.AppNavHost
import com.ccread.reader.ui.theme.JingduTheme
import org.readium.r2.navigator.epub.EpubNavigatorFragment

class MainActivity : FragmentActivity() {

    private var incomingUri by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        // Readium's navigator has no no-arg constructor. If the activity is restored
        // after process death, the default factory would crash; replace it with a
        // plain fragment and drop the stale instance before Compose re-opens the book.
        supportFragmentManager.fragmentFactory = SafeFragmentFactory()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        supportFragmentManager.findFragmentByTag(EPUB_TAG)?.let { stale ->
            supportFragmentManager.beginTransaction()
                .remove(stale)
                .commitNowAllowingStateLoss()
        }
        incomingUri = intent.viewedUri()
        setContent {
            JingduTheme {
                AppNavHost(
                    incomingUri = incomingUri,
                    onIncomingConsumed = { incomingUri = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingUri = intent.viewedUri()
    }

    companion object {
        const val EPUB_TAG = "epub-navigator"
    }
}

private fun Intent?.viewedUri(): Uri? =
    this?.takeIf { action == Intent.ACTION_VIEW }?.data

private class SafeFragmentFactory : FragmentFactory() {
    override fun instantiate(classLoader: ClassLoader, className: String): Fragment =
        if (className == EpubNavigatorFragment::class.java.name) {
            Fragment()
        } else {
            super.instantiate(classLoader, className)
        }
}
