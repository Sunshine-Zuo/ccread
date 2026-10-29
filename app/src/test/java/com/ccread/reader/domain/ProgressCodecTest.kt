package com.ccread.reader.domain

import com.ccread.reader.domain.model.EpubProgress
import com.ccread.reader.domain.model.ProgressCodec
import com.ccread.reader.domain.model.TxtProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProgressCodecTest {

    @Test
    fun `txt progress round trips`() {
        val progress = TxtProgress(percent = 12.34, chapterIndex = 3, charOffsetInChapter = 120)
        assertEquals(progress, ProgressCodec.decode(ProgressCodec.encode(progress)))
    }

    @Test
    fun `epub progress round trips`() {
        val progress = EpubProgress(percent = 42.0, locatorJson = """{"href":"ch1.xhtml"}""")
        assertEquals(progress, ProgressCodec.decode(ProgressCodec.encode(progress)))
    }

    @Test
    fun `invalid json decodes to null`() {
        assertNull(ProgressCodec.decode("not json"))
        assertNull(ProgressCodec.decode(null))
    }
}
