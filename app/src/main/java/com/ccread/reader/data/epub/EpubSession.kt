package com.ccread.reader.data.epub

import android.content.Context
import android.net.Uri
import com.ccread.reader.domain.model.TocNode
import java.io.Closeable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.epub.EpubParser
import org.json.JSONObject

class EpubSession(private val context: Context) : Closeable {

    var publication: Publication? = null
        private set

    suspend fun open(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val retriever = AssetRetriever(context.contentResolver, DefaultHttpClient())
            val absoluteUrl = AbsoluteUrl(uri.toString()) ?: return@withContext false
            val asset = retriever.retrieve(absoluteUrl).getOrNull()
                ?: return@withContext false
            val opener = PublicationOpener(EpubParser())
            val opened = opener.open(asset, allowUserInteraction = false).getOrNull()
                ?: return@withContext false
            publication = opened
            true
        }.getOrDefault(false)
    }

    fun bookTitle(): String = publication?.metadata?.title ?: ""

    fun tocTree(): List<TocNode> = publication?.tableOfContents?.map { it.toNode() } ?: emptyList()

    fun percentage(locatorJson: String): Double {
        val locator = parseLocator(locatorJson) ?: return 0.0
        return (locator.locations?.totalProgression ?: 0.0) * 100.0
    }

    override fun close() {
        runCatching { publication?.close() }
        publication = null
    }

    companion object {
        fun parseLocator(json: String): Locator? =
            runCatching { Locator.fromJSON(JSONObject(json)) }.getOrNull()

        fun titleIn(tree: List<TocNode>, href: String): String? {
            var exact: String? = null
            fun walkExact(nodes: List<TocNode>) {
                for (node in nodes) {
                    if (node.href == href) exact = node.title
                    walkExact(node.children)
                }
            }
            walkExact(tree)
            if (exact != null) return exact

            val target = href.substringBefore('#')
            var found: String? = null
            fun walk(nodes: List<TocNode>) {
                for (node in nodes) {
                    val nodeHref = node.href?.substringBefore('#')
                    if (nodeHref != null &&
                        (nodeHref == target || target.endsWith(nodeHref) || nodeHref.endsWith(target))
                    ) {
                        found = node.title
                    }
                    walk(node.children)
                }
            }
            walk(tree)
            return found
        }
    }
}

private fun Link.toNode(): TocNode = TocNode(
    title = title ?: href.toString(),
    href = href.toString(),
    children = children.map { it.toNode() },
)
