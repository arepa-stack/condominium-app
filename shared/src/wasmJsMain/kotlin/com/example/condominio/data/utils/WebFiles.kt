package com.example.condominio.data.utils

import kotlinx.browser.document
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.get
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.url.URL
import org.w3c.files.FileReader
import org.w3c.files.get

// Browser has no file paths: picked files live here, keyed by their blob: URL.
// ponytail: never evicted; fine for a few uploads per session, revoke URLs if memory matters.
private val pickedBytes = mutableMapOf<String, ByteArray>()
private val pickedNames = mutableMapOf<String, String>()

/** Opens the native file dialog; returns (blobUrl, mimeType) once the bytes are loaded. */
internal fun pickFile(accept: String, onResult: (url: String?, mimeType: String?) -> Unit) {
    val input = document.createElement("input") as HTMLInputElement
    input.type = "file"
    input.accept = accept
    input.addEventListener("change", {
        val file = input.files?.get(0)
        if (file == null) {
            onResult(null, null)
        } else {
            val reader = FileReader()
            reader.addEventListener("load", {
                val buffer = Int8Array(reader.result!!.unsafeCast<ArrayBuffer>())
                val url = URL.createObjectURL(file)
                pickedBytes[url] = ByteArray(buffer.length) { buffer[it] }
                pickedNames[url] = file.name
                onResult(url, file.type.ifBlank { null })
            })
            reader.addEventListener("error", { onResult(null, null) })
            reader.readAsArrayBuffer(file)
        }
    })
    input.click()
}

class WebFileReader : PlatformFileReader {
    override fun readBytes(path: String): ByteArray? = pickedBytes[path]
    override fun getFileName(path: String): String = pickedNames[path] ?: "upload"
}
