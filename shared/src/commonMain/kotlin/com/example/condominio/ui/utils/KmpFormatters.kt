package com.example.condominio.ui.utils

expect fun formatCurrency(amount: Double): String

expect fun formatDate(timestamp: Long, formatString: String = "MMM dd, yyyy"): String

expect fun formatMonthYear(timestamp: Long): String

/** Common stand-in for JVM String.format on resource strings: fills `%1$s` / `%1$d` placeholders and `%%`. */
fun String.formatArgs(vararg args: Any?): String =
    args.foldIndexed(this) { i, acc, arg ->
        acc.replace("%${i + 1}\$s", arg.toString()).replace("%${i + 1}\$d", arg.toString())
    }.replace("%%", "%")

/** False on Kotlin/Wasm: the canvas renderer ships no emoji font, so emoji draw as empty boxes. */
expect val platformRendersEmoji: Boolean

/** Strips emoji (incl. flags) where the platform can't draw them; unchanged on Android/iOS. */
// Char filter instead of Regex: Kotlin/Wasm regex works on code points, so surrogate ranges never match.
fun String.emojiSafe(): String = if (platformRendersEmoji) this else buildString {
    for (c in this@emojiSafe) if (!c.isSurrogate() && c !in '\u2600'..'\u27BF' && c != '\uFE0F') append(c)
}.trim()
