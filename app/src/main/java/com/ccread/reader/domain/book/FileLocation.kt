package com.ccread.reader.domain.book

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.system.Os

object FileLocation {

    fun resolve(context: Context, uriString: String): String {
        val direct = display(uriString)
        if (direct.startsWith("/")) return direct

        val uri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return direct
        if (uri.scheme != "content") return direct
        val resolver = context.contentResolver

        runCatching {
            resolver.openFileDescriptor(uri, "r")?.use { descriptor ->
                val link = Os.readlink("/proc/self/fd/${descriptor.fd}")
                if (!link.isNullOrBlank() && link.startsWith("/") && !link.startsWith("/proc")) {
                    return normalize(link)
                }
            }
        }

        runCatching {
            resolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    for (index in 0 until cursor.columnCount) {
                        val column = cursor.getColumnName(index).lowercase()
                        if ((column.contains("path") || column == "_data") && !cursor.isNull(index)) {
                            val value = cursor.getString(index)
                            if (!value.isNullOrBlank() && value.startsWith("/")) return value
                        }
                    }
                }
            }
        }

        runCatching {
            resolver.query(uri, arrayOf(MediaStore.MediaColumns.DATA), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst() && !cursor.isNull(0)) {
                    val data = cursor.getString(0)
                    if (!data.isNullOrBlank() && data.startsWith("/")) return data
                }
            }
        }

        val documentId = runCatching { DocumentsContract.getDocumentId(uri) }.getOrNull()
        val decodedId = documentId?.let { Uri.decode(it) }
        val mediaId = decodedId
            ?.removePrefix("msf:")
            ?.removePrefix("document:")
            ?.takeIf { it.matches(Regex("\\d+")) }

        if (mediaId != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val collections = listOf(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                MediaStore.Files.getContentUri("external"),
            )
            for (collection in collections) {
                runCatching {
                    resolver.query(
                        collection,
                        arrayOf(
                            MediaStore.MediaColumns.DATA,
                            MediaStore.MediaColumns.RELATIVE_PATH,
                            MediaStore.MediaColumns.DISPLAY_NAME,
                        ),
                        "_id=?",
                        arrayOf(mediaId),
                        null,
                    )?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val data = if (cursor.isNull(0)) null else cursor.getString(0)
                            if (!data.isNullOrBlank() && data.startsWith("/")) return data
                            val relative = if (cursor.isNull(1)) null else cursor.getString(1)
                            val name = if (cursor.isNull(2)) null else cursor.getString(2)
                            if (!relative.isNullOrBlank() && !name.isNullOrBlank()) {
                                return "/storage/emulated/0/${relative.trimEnd('/')}/$name"
                            }
                        }
                    }
                }
            }
        }

        val displayName = runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getString(0) else null
            }
        }.getOrNull()

        if (!displayName.isNullOrBlank() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching {
                resolver.query(
                    MediaStore.Files.getContentUri("external"),
                    arrayOf(MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.RELATIVE_PATH),
                    "${MediaStore.MediaColumns.DISPLAY_NAME}=?",
                    arrayOf(displayName),
                    null,
                )?.use { cursor ->
                    while (cursor.moveToNext()) {
                        val data = if (cursor.isNull(0)) null else cursor.getString(0)
                        if (!data.isNullOrBlank() && data.startsWith("/")) return data
                        val relative = if (cursor.isNull(1)) null else cursor.getString(1)
                        if (!relative.isNullOrBlank()) {
                            return "/storage/emulated/0/${relative.trimEnd('/')}/$displayName"
                        }
                    }
                }
            }
        }

        if (decodedId != null &&
            decodedId.contains('/') &&
            !decodedId.startsWith("/") &&
            !decodedId.startsWith("raw:") &&
            !decodedId.startsWith("msf:") &&
            !decodedId.startsWith("home:") &&
            !decodedId.startsWith("primary:")
        ) {
            return "/storage/emulated/0/$decodedId"
        }

        return direct
    }

    fun display(uriString: String): String {
        val uri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return uriString
        return when (uri.scheme) {
            "file" -> uri.path ?: uriString
            "content" -> displayDocument(uri)
            else -> Uri.decode(uriString)
        }
    }

    private fun normalize(path: String): String = when {
        path.startsWith("/mnt/user/0/primary/") ->
            "/storage/emulated/0/" + path.removePrefix("/mnt/user/0/primary/")

        path.startsWith("/data/media/0/") ->
            "/storage/emulated/0/" + path.removePrefix("/data/media/0/")

        else -> path
    }

    private fun displayDocument(uri: Uri): String {
        val documentId = runCatching { DocumentsContract.getDocumentId(uri) }.getOrNull()
            ?: return Uri.decode(uri.toString())
        val decoded = Uri.decode(documentId)
        return when {
            decoded.startsWith("primary:") ->
                "/storage/emulated/0/" + decoded.removePrefix("primary:")

            decoded.startsWith("home:") ->
                "/storage/emulated/0/Documents/" + decoded.removePrefix("home:")

            decoded.startsWith("raw:") ->
                decoded.removePrefix("raw:")

            else -> decoded
        }
    }
}
