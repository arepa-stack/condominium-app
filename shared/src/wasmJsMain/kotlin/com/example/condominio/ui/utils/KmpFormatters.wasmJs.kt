package com.example.condominio.ui.utils

import com.example.condominio.ui.locale.LocaleManager
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private fun toFixed2(amount: Double): String = js("amount.toFixed(2)")

private fun monthName(timestamp: Double, locale: String, style: String): String =
    js("new Date(timestamp).toLocaleString(locale, { month: style })")

actual fun formatCurrency(amount: Double): String = toFixed2(amount)

// Supports the tokens the app uses: yyyy, MMMM, MMM, MM, dd.
actual fun formatDate(timestamp: Long, formatString: String): String {
    val date = Instant.fromEpochMilliseconds(timestamp).toLocalDateTime(TimeZone.currentSystemDefault())
    val lang = LocaleManager.currentLanguage.code
    return Regex("yyyy|MMMM|MMM|MM|dd").replace(formatString) {
        when (it.value) {
            "yyyy" -> date.year.toString()
            "MMMM" -> monthName(timestamp.toDouble(), lang, "long")
            "MMM" -> monthName(timestamp.toDouble(), lang, "short")
            "MM" -> date.monthNumber.toString().padStart(2, '0')
            else -> date.dayOfMonth.toString().padStart(2, '0')
        }
    }
}

actual fun formatMonthYear(timestamp: Long): String =
    monthName(timestamp.toDouble(), "es-ES", "long").replaceFirstChar { it.uppercase() }
