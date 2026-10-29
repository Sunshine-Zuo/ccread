package com.ccread.reader.domain.book

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class FileLocationTest {

    @Test
    fun `external storage document id becomes a readable path`() {
        val uri = "content://com.android.externalstorage.documents/document/primary%3ADownload%2F%E4%B8%89%E5%9B%BD.txt"

        assertEquals("/storage/emulated/0/Download/三国.txt", FileLocation.display(uri))
    }

    @Test
    fun `non content uri is decoded as is`() {
        assertEquals("/sdcard/books/a.txt", FileLocation.display("file:///sdcard/books/a.txt"))
    }

    @Test
    fun `unknown document provider falls back to its decoded document id`() {
        val uri = "content://com.example.provider/document/books%2Fa.epub"

        assertEquals("books/a.epub", FileLocation.display(uri))
    }
}
