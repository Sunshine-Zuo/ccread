package com.ccread.reader.domain.txt

import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

object CharsetDetector {

    fun detect(prefix: ByteArray): Charset {
        if (prefix.size >= 3 &&
            prefix[0] == 0xEF.toByte() &&
            prefix[1] == 0xBB.toByte() &&
            prefix[2] == 0xBF.toByte()
        ) {
            return Charsets.UTF_8
        }
        if (prefix.size >= 2) {
            val b0 = prefix[0].toInt() and 0xFF
            val b1 = prefix[1].toInt() and 0xFF
            if (b0 == 0xFF && b1 == 0xFE) return Charsets.UTF_16LE
            if (b0 == 0xFE && b1 == 0xFF) return Charsets.UTF_16BE
        }
        return if (isValidUtf8(prefix)) Charsets.UTF_8 else Charset.forName("GB18030")
    }

    private fun isValidUtf8(bytes: ByteArray): Boolean {
        if (bytes.isEmpty()) return true
        val decoder = Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        // A prefix read may cut the last multi-byte character; retry without up to 3 trailing bytes.
        for (trim in 0..3) {
            val length = bytes.size - trim
            if (length <= 0) break
            try {
                decoder.reset()
                decoder.decode(ByteBuffer.wrap(bytes, 0, length))
                return true
            } catch (_: CharacterCodingException) {
                // try a shorter prefix
            }
        }
        return false
    }
}
