package com.example.condominio.ui.utils

expect fun formatCurrency(amount: Double): String

/**
 * Parses a typed money amount, accepting comma or dot decimals and grouping: "150,50", "150.50",
 * "1.500,25", "1,500.25", "1500". A lone separator followed by exactly 3 digits is grouping ("1.500" = 1500).
 * Returns null for anything else (letters, more than 2 decimals, malformed groups).
 * Never feed [formatCurrency] output back here; use [formatAmountInput] to prefill fields.
 */
fun parseAmount(input: String): Double? {
    val s = input.trim().replace(" ", "")
    if (s.isEmpty() || s.any { !it.isDigit() && it != '.' && it != ',' }) return null
    val lastSep = maxOf(s.lastIndexOf('.'), s.lastIndexOf(','))
    if (lastSep < 0) return s.toDoubleOrNull()
    val sep = s[lastSep]
    val other = if (sep == '.') ',' else '.'
    val single = s.count { it == sep } == 1
    // Decimal separator: the last one when both kinds appear, or a lone one not followed by 3 digits.
    val isDecimal = (other in s || s.length - lastSep - 1 != 3) && single
    val intPart = if (isDecimal) s.substring(0, lastSep) else s
    val decPart = if (isDecimal) s.substring(lastSep + 1) else ""
    if (isDecimal && (decPart.isEmpty() || decPart.length > 2)) return null
    val groups = intPart.split('.', ',')
    if (groups.size > 1 && (groups.first().isEmpty() || groups.first().length > 3 || groups.drop(1).any { it.length != 3 })) return null
    return (groups.joinToString("") + if (isDecimal) ".$decPart" else "").toDoubleOrNull()
}

/** Plain "1500.25" (dot, 2 decimals, no grouping) for prefilling an amount field; parseable by [parseAmount]. */
fun formatAmountInput(amount: Double): String {
    val cents = kotlin.math.round(amount * 100).toLong()
    return "${cents / 100}.${(cents % 100).toString().padStart(2, '0')}"
}

/** Venezuelan bolívar format, same on every platform: 9901.084 -> "9.901,08" (dot grouping, comma decimals). */
fun formatBs(amount: Double): String {
    val cents = kotlin.math.round(amount * 100).toLong()
    val grouped = (cents / 100).toString().reversed().chunked(3).joinToString(".").reversed()
    return "$grouped,${(cents % 100).toString().padStart(2, '0')}"
}

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

/** True on the web target: used to try web-only visual variants (e.g. the admin palette POC). */
expect val isWebPlatform: Boolean
