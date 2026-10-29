package com.example.condominio.ui.utils

/** Escapes user/API text (descriptions, references, names) before it goes into the receipt HTML. */
internal fun escapeHtml(text: String): String = text
    .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    .replace("\"", "&quot;").replace("'", "&#39;")

/**
 * Printable receipt page: [header] (building · unit), [title], label/value [rows], an optional [table]
 * (first row is the header) and a [footer]. Self-contained (inline CSS) so it prints the same on web and Android.
 */
internal fun receiptHtml(
    header: String,
    title: String,
    rows: List<Pair<String, String>>,
    table: List<List<String>> = emptyList(),
    footer: String
): String = buildString {
    append("<!doctype html><html><head><meta charset=\"utf-8\"><title>")
    append(escapeHtml(title))
    append("</title><style>")
    append("body{font-family:Arial,Helvetica,sans-serif;color:#1d1b16;margin:40px;font-size:14px}")
    append(".hdr{color:#6b5e4f;font-size:12px;letter-spacing:.08em;text-transform:uppercase}")
    append("h1{font-size:22px;margin:6px 0 20px}")
    append("table{width:100%;border-collapse:collapse;margin-bottom:24px}")
    append("td,th{padding:8px 4px;border-bottom:1px solid #e6dfd6;text-align:left}")
    append("th{font-size:11px;color:#6b5e4f;text-transform:uppercase}")
    append(".kv td:first-child{color:#6b5e4f;width:40%}.kv td:last-child{font-weight:bold}")
    append(".ftr{color:#8a7f72;font-size:11px;margin-top:32px}")
    append("</style></head><body>")
    append("<div class=\"hdr\">").append(escapeHtml(header)).append("</div>")
    append("<h1>").append(escapeHtml(title)).append("</h1>")
    append("<table class=\"kv\">")
    rows.forEach { (label, value) ->
        append("<tr><td>").append(escapeHtml(label)).append("</td><td>").append(escapeHtml(value)).append("</td></tr>")
    }
    append("</table>")
    if (table.size > 1) {
        append("<table>")
        table.forEachIndexed { i, cells ->
            val tag = if (i == 0) "th" else "td"
            append("<tr>")
            cells.forEach { append("<$tag>").append(escapeHtml(it)).append("</$tag>") }
            append("</tr>")
        }
        append("</table>")
    }
    append("<div class=\"ftr\">").append(escapeHtml(footer)).append("</div>")
    append("</body></html>")
}
