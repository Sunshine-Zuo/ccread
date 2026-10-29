package com.ccread.reader.ui.reader

import java.util.Base64
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ImageDataTest {

    @Test
    fun `decodes the base64 payload of a data url`() {
        val payload = Base64.getEncoder().encodeToString(byteArrayOf(1, 2, 3, 4))

        assertArrayEquals(
            byteArrayOf(1, 2, 3, 4),
            ImageData.decode("data:image/webp;base64,$payload"),
        )
    }

    @Test
    fun `returns null when the data url has no payload`() {
        assertNull(ImageData.decode("data:image/webp;base64"))
    }

    @Test
    fun `returns null when the payload is not valid base64`() {
        assertNull(ImageData.decode("data:image/webp;base64,!!!!"))
    }
}
