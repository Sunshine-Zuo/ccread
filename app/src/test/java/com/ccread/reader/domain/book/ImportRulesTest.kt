package com.ccread.reader.domain.book

import com.ccread.reader.domain.model.BookFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ImportRulesTest {

    @Test
    fun `extension and mime decide the format`() {
        assertEquals(BookFormat.TXT, ImportRules.formatOf("a.TXT", null))
        assertEquals(BookFormat.EPUB, ImportRules.formatOf("b.epub", "application/epub+zip"))
        assertEquals(BookFormat.TXT, ImportRules.formatOf("c", "text/plain"))
        assertEquals(BookFormat.EPUB, ImportRules.formatOf("d", "application/epub+zip"))
        assertNull(ImportRules.formatOf("c.pdf", "application/pdf"))
    }

    @Test
    fun `limits are 200MB and 500MB`() {
        assertEquals(200L * 1024 * 1024, ImportRules.limitBytes(BookFormat.TXT))
        assertEquals(500L * 1024 * 1024, ImportRules.limitBytes(BookFormat.EPUB))
    }

    @Test
    fun `title drops only the last extension`() {
        assertEquals("三国演义.全本", ImportRules.titleOf("三国演义.全本.txt"))
        assertEquals("no-extension", ImportRules.titleOf("no-extension"))
        assertEquals("", ImportRules.titleOf(".txt"))
    }
}
