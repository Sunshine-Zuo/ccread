package com.ccread.reader.domain.txt

data class ChapterRule(
    val name: String,
    val pattern: Regex,
    val titleGroup: Int = 0,
    val enabled: Boolean = true,
)

object BuiltInChapterRules {
    val all: List<ChapterRule> = listOf(
        ChapterRule(
            name = "第X章",
            pattern = Regex("""^\s{0,4}第[0-9零〇一二三四五六七八九十百千万两]+[章回节集篇部卷]\s{0,2}.{0,30}$"""),
        ),
        ChapterRule(
            name = "特殊章节",
            pattern = Regex("""^\s{0,4}(序章|楔子|前言|序言|引子|尾声|后记|番外)(\s+.{0,30})?$"""),
        ),
        ChapterRule(
            name = "Chapter",
            pattern = Regex("""^\s{0,4}(Chapter|CHAPTER)\s+\d{1,4}.{0,30}$"""),
        ),
        ChapterRule(
            name = "数字序号",
            pattern = Regex("""^\d{1,4}[、.．].{0,20}$"""),
            enabled = false,
        ),
    )
}
