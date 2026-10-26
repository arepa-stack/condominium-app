package com.example.condominio.ui.screens.billboard

import androidx.compose.ui.platform.UriHandler

// ---------------------------------------------------------------------------
// Attachment type detection
// ---------------------------------------------------------------------------

private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp")
private val PDF_EXTENSIONS = setOf("pdf")

private fun attachmentExtension(url: String): String {
    val path = url.substringBefore('?')
    val filename = path.substringAfterLast('/')
    if (!filename.contains('.')) return ""
    return filename.substringAfterLast('.').lowercase()
}

/** Only a known image extension counts: no extension is a generic file, not a broken image (E-C1). */
internal fun isImage(url: String): Boolean = attachmentExtension(url) in IMAGE_EXTENSIONS

internal fun isPdf(url: String) = attachmentExtension(url) in PDF_EXTENSIONS

/** File name of the attachment as the user should read it ("%20" -> " "); empty when the URL has none. */
internal fun attachmentFileName(url: String): String =
    percentDecode(url.substringBefore('?').substringAfterLast('/'))

/** UTF-8 percent-decoding; malformed escapes are kept literally. */
internal fun percentDecode(text: String): String {
    if ('%' !in text) return text
    val bytes = ArrayList<Byte>(text.length)
    val plain = StringBuilder()
    fun flushPlain() {
        if (plain.isNotEmpty()) {
            plain.toString().encodeToByteArray().forEach { bytes.add(it) }
            plain.clear()
        }
    }
    var i = 0
    while (i < text.length) {
        val c = text[i]
        val pair = if (c == '%' && i + 2 < text.length) text.substring(i + 1, i + 3) else null
        val byte = pair?.takeIf { p -> p.all { it in '0'..'9' || it.lowercaseChar() in 'a'..'f' } }?.toInt(16)
        if (byte != null) {
            flushPlain()
            bytes.add(byte.toByte())
            i += 3
        } else {
            plain.append(c)
            i++
        }
    }
    flushPlain()
    return bytes.toByteArray().decodeToString()
}

// ---------------------------------------------------------------------------
// External links (APP-M3): only http/https may leave the app
// ---------------------------------------------------------------------------

internal fun isSafeExternalUrl(url: String): Boolean {
    val u = url.trim().lowercase()
    return u.startsWith("http://") || u.startsWith("https://")
}

/** A [UriHandler] that ignores any link that is not http/https (announcement text and attachments are user content). */
internal fun safeUriHandler(base: UriHandler): UriHandler = object : UriHandler {
    override fun openUri(uri: String) {
        if (isSafeExternalUrl(uri)) base.openUri(uri)
    }
}
