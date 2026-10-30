package com.ccread.reader.data.file

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.ccread.reader.domain.model.BookFormat
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BookFileStoreTest {

    private lateinit var context: Context
    private lateinit var store: BookFileStore

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        store = BookFileStore(context)
    }

    private fun sourceFile(content: String, name: String = "novel.txt"): Uri {
        val file = File(context.cacheDir, name)
        file.writeText(content)
        return Uri.fromFile(file)
    }

    @Test
    fun `copy puts the content in app private storage`() {
        val source = sourceFile("第一章 开始")
        val target = store.targetFor(source, BookFormat.TXT)

        assertTrue(store.copy(source, target))

        val copied = File(requireNotNull(target.path))
        assertTrue(copied.exists())
        assertEquals("第一章 开始", copied.readText())
        assertTrue(store.owns(target))
        assertFalse(store.owns(source))
    }

    @Test
    fun `target is stable for the same source and format`() {
        val source = Uri.parse("content://com.tencent.mm.fileprovider/1/novel.txt")

        assertEquals(
            store.targetFor(source, BookFormat.TXT),
            store.targetFor(source, BookFormat.TXT),
        )
        assertTrue(store.targetFor(source, BookFormat.TXT).path!!.endsWith(".txt"))
    }

    @Test
    fun `delete owned copy removes the private file`() {
        val source = sourceFile("abc")
        val target = store.targetFor(source, BookFormat.TXT)
        store.copy(source, target)

        assertTrue(store.deleteOwned(target))
        assertFalse(File(requireNotNull(target.path)).exists())
    }

    @Test
    fun `deleting a foreign file leaves it untouched`() {
        val source = sourceFile("abc")

        assertTrue(store.deleteOwned(source))
        assertTrue(File(requireNotNull(source.path)).exists())
    }
}
