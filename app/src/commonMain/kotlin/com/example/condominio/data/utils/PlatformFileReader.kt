package com.example.condominio.data.utils

interface PlatformFileReader {
    fun readBytes(path: String): ByteArray?
    fun getFileName(path: String): String
}
