package com.example.condominio.ui.utils

expect fun formatCurrency(amount: Double): String

expect fun formatDate(timestamp: Long, formatString: String = "MMM dd, yyyy"): String

expect fun formatMonthYear(timestamp: Long): String

/** Common stand-in for JVM String.format on resource strings: fills `%1$s` / `%1$d` placeholders and `%%`. */
fun String.formatArgs(vararg args: Any?): String =
    args.foldIndexed(this) { i, acc, arg ->
        acc.replace("%${i + 1}\$s", arg.toString()).replace("%${i + 1}\$d", arg.toString())
    }.replace("%%", "%")
