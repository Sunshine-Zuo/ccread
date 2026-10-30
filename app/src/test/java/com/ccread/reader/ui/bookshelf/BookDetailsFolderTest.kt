package com.ccread.reader.ui.bookshelf

import android.provider.DocumentsContract
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BookDetailsFolderTest {

    @Test
    fun `external storage file opens its parent folder`() {
        val intent = folderIntent("/storage/emulated/0/Download/三国.txt", "")

        assertEquals(
            "content://com.android.externalstorage.documents/document/primary%3ADownload",
            intent?.data.toString(),
        )
        assertEquals(DocumentsContract.Document.MIME_TYPE_DIR, intent?.type)
    }

    @Test
    fun `secondary volume uses its volume id`() {
        val intent = folderIntent("/storage/1234-5678/Books/a.epub", "")

        assertEquals(
            "content://com.android.externalstorage.documents/document/1234-5678%3ABooks",
            intent?.data.toString(),
        )
    }

    @Test
    fun `sdcard aliases map to primary storage`() {
        assertEquals(
            "content://com.android.externalstorage.documents/document/primary%3ADownload",
            folderIntent("/sdcard/Download/a.txt", "")?.data.toString(),
        )
        assertEquals(
            "content://com.android.externalstorage.documents/document/primary%3ABooks",
            folderIntent("/mnt/sdcard/Books/a.txt", "")?.data.toString(),
        )
    }

    @Test
    fun `content uri derives its parent when the path is unknown`() {
        val intent = folderIntent(
            "msf:1000000123",
            "content://com.android.externalstorage.documents/document/primary%3ADownload%2Fa.txt",
        )

        assertEquals(
            "content://com.android.externalstorage.documents/document/primary%3ADownload",
            intent?.data.toString(),
        )
    }

    @Test
    fun `downloads document falls back to the downloads root`() {
        val intent = folderIntent(
            "msf:1000000123",
            "content://com.android.providers.downloads.documents/document/msf%3A1000000123",
        )

        assertEquals(
            "content://com.android.providers.downloads.documents/root/downloads",
            intent?.data.toString(),
        )
    }

    @Test
    fun `app private storage has no system folder`() {
        assertNull(
            folderIntent(
                "/data/user/0/com.ccread.reader/files/books/a.txt",
                "file:///data/user/0/com.ccread.reader/files/books/a.txt",
            ),
        )
    }
}
