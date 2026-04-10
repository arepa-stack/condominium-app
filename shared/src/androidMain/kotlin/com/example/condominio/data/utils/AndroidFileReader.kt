package com.example.condominio.data.utils

import java.io.File

class AndroidFileReader : PlatformFileReader {
    override fun readBytes(path: String): ByteArray? {
        val file = File(path)
        return if (file.exists()) file.readBytes() else null
    }

    override fun getFileName(path: String): String {
        return File(path).name
    }
}
