package com.ccread.reader.domain.txt

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.em

/** TXT 段落首行缩进两个汉字宽；文件自带的段首空白会先被去掉，避免叠加。 */
object TxtIndent {

    private val paragraphStyle = ParagraphStyle(textIndent = TextIndent(firstLine = 2.em))

    fun page(fullText: String, start: Int, end: Int): AnnotatedString {
        val safeStart = start.coerceIn(0, fullText.length)
        val safeEnd = end.coerceIn(safeStart, fullText.length)
        if (safeStart == safeEnd) return AnnotatedString("")

        val display = StringBuilder(safeEnd - safeStart)
        var indentStart = -1
        var paragraphStart = startsInLeadingSpace(fullText, safeStart)
        var index = safeStart
        while (index < safeEnd) {
            val char = fullText[index]
            if (paragraphStart && isIndentSpace(char)) {
                index++
                continue
            }
            if (paragraphStart) {
                if (indentStart < 0) indentStart = display.length
                paragraphStart = false
            }
            display.append(char)
            if (char == '\n') paragraphStart = true
            index++
        }
        return withIndent(display.toString(), indentStart)
    }

    fun paragraph(text: String): AnnotatedString {
        val firstVisible = text.indexOfFirst { !isIndentSpace(it) }
        val display = when {
            firstVisible < 0 -> ""
            firstVisible == 0 -> text
            else -> text.substring(firstVisible)
        }
        return withIndent(display, 0)
    }

    private fun withIndent(text: String, indentStart: Int): AnnotatedString {
        if (indentStart < 0 || indentStart >= text.length) return AnnotatedString(text)
        return buildAnnotatedString {
            append(text)
            addStyle(paragraphStyle, indentStart, text.length)
        }
    }

    /** 页面起点是否落在段首空白里（页面从空白中间开始也视为段首）。 */
    private fun startsInLeadingSpace(text: String, start: Int): Boolean {
        if (start == 0) return true
        var lineStart = start
        while (lineStart > 0 && text[lineStart - 1] != '\n') lineStart--
        for (index in lineStart until start) {
            if (!isIndentSpace(text[index])) return false
        }
        return true
    }

    private fun isIndentSpace(char: Char): Boolean =
        char == ' ' || char == '\t' || char == '　' || char == ' '
}
