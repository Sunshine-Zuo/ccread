package com.ccread.reader.domain.txt

import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.em
import org.junit.Assert.assertEquals
import org.junit.Test

class TxtIndentTest {

    private val twoChars = TextIndent(firstLine = 2.em)

    @Test
    fun `page starting at a paragraph start indents from the first character`() {
        val annotated = TxtIndent.page("abc\ndef", 0, 7)

        val style = annotated.paragraphStyles.single()
        assertEquals(0, style.start)
        assertEquals(7, style.end)
        assertEquals(ParagraphStyle(textIndent = twoChars), style.item)
    }

    @Test
    fun `page starting mid-paragraph indents from the next paragraph`() {
        val annotated = TxtIndent.page("abc\ndef", 2, 7)

        val style = annotated.paragraphStyles.single()
        assertEquals(2, style.start)
        assertEquals(5, style.end)
    }

    @Test
    fun `page without a new paragraph gets no indent`() {
        assertEquals(0, TxtIndent.page("abcdef", 2, 6).paragraphStyles.size)
        assertEquals(0, TxtIndent.page("abc\ndef", 2, 4).paragraphStyles.size)
    }

    @Test
    fun `paragraph drops existing spaces before applying the indent`() {
        val annotated = TxtIndent.paragraph("　　第一段")

        assertEquals("第一段", annotated.text)
        val style = annotated.paragraphStyles.single()
        assertEquals(0, style.start)
        assertEquals(3, style.end)
        assertEquals(ParagraphStyle(textIndent = twoChars), style.item)
    }

    @Test
    fun `paragraph with ascii spaces is normalized too`() {
        assertEquals("第一段", TxtIndent.paragraph("  第一段").text)
        assertEquals("第一段", TxtIndent.paragraph("\t第一段").text)
    }

    @Test
    fun `page drops spaces at every paragraph start`() {
        val text = "　　第一段\n　　第二段"

        val annotated = TxtIndent.page(text, 0, text.length)

        assertEquals("第一段\n第二段", annotated.text)
        assertEquals(0, annotated.paragraphStyles.single().start)
    }

    @Test
    fun `page starting inside an existing indent indents from the paragraph text`() {
        val annotated = TxtIndent.page("　　第一段", 1, 5)

        assertEquals("第一段", annotated.text)
        assertEquals(0, annotated.paragraphStyles.single().start)
    }

    @Test
    fun `a continued paragraph is not indented while the next one is`() {
        val text = "开头\n　　第二段"

        val annotated = TxtIndent.page(text, 1, text.length)

        assertEquals("头\n第二段", annotated.text)
        assertEquals(2, annotated.paragraphStyles.single().start)
    }
}
