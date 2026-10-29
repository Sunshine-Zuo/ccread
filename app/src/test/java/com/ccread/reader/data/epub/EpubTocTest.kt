package com.ccread.reader.data.epub

import com.ccread.reader.domain.model.TocNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EpubTocTest {

    @Test
    fun `finds chapter titles in a nested toc`() {
        val tree = listOf(
            TocNode("第一卷", "a.xhtml", listOf(TocNode("第一章", "b.xhtml"))),
        )

        assertEquals("第一卷", EpubSession.titleIn(tree, "a.xhtml"))
        assertEquals("第一章", EpubSession.titleIn(tree, "b.xhtml#id2"))
    }

    @Test
    fun `exact fragment match wins over path order`() {
        val tree = listOf(
            TocNode("第二节", "ch1.xhtml#s2"),
            TocNode("第一节", "ch1.xhtml#s1"),
        )

        assertEquals("第二节", EpubSession.titleIn(tree, "ch1.xhtml#s2"))
    }

    @Test
    fun `unknown href returns null`() {
        assertNull(EpubSession.titleIn(emptyList(), "x"))
    }
}
