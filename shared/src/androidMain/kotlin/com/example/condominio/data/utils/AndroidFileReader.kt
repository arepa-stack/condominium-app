package com.example.condominio.data.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File

class AndroidFileReader(private val context: Context) : PlatformFileReader {
    override fun readBytes(path: String): ByteArray? {
        return if (path.startsWith("content://") || path.startsWith("file://")) {
            runCatching {
                context.contentResolver.openInputStream(Uri.parse(path))?.use { it.readBytes() }
            }.getOrNull()
        } else {
            val file = File(path)
            if (file.exists()) file.readBytes() else null
        }
    }

    override fun getFileName(path: String): String {
        if (path.startsWith("content://") || path.startsWith("file://")) {
            val uri = Uri.parse(path)
            runCatching {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0 && cursor.moveToFirst()) {
                        return cursor.getString(nameIndex)
                    }
                }
            }
            return uri.lastPathSegment ?: "upload.jpg"
        }
        return File(path).name
    }
}
