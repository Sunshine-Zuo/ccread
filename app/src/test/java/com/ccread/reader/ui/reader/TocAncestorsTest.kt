package com.ccread.reader.ui.reader

import com.ccread.reader.domain.model.TocNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TocAncestorsTest {

    private val tree = listOf(
        TocNode(
            title = "第一卷",
            href = "v1.xhtml",
            children = listOf(
                TocNode("第一章", "c1.xhtml"),
                TocNode("第二章", "c2.xhtml"),
            ),
        ),
    )

    @Test
    fun `returns only the parent keys`() {
        assertEquals(setOf("v1.xhtml"), TocAncestors.of(tree, "c2.xhtml#frag"))
    }

    @Test
    fun `a top level node has no ancestors`() {
        assertTrue(TocAncestors.of(tree, "v1.xhtml").isEmpty())
    }
}
