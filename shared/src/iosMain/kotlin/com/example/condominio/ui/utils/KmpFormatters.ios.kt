package com.example.condominio.ui.utils

import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterDecimalStyle
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale

// Same output as Android/web ("1500.25"): fixed locale, no grouping, no currency symbol (the symbol comes from
// `currency_amount`). The old currency style added locale grouping and could keep dots from symbols like "Bs.".
actual fun formatCurrency(amount: Double): String {
    val formatter = NSNumberFormatter().apply {
        numberStyle = NSNumberFormatterDecimalStyle
        locale = NSLocale("en_US_POSIX")
        usesGroupingSeparator = false
        minimumFractionDigits = 2u
        maximumFractionDigits = 2u
    }
    return formatter.stringFromNumber(NSNumber(amount)) ?: amount.toString()
}

actual fun formatDate(timestamp: Long, formatString: String): String {
    val date = NSDate(timeIntervalSinceReferenceDate = (timestamp / 1000.0) - 978307200.0)
    val formatter = NSDateFormatter().apply {
        dateFormat = formatString
        locale = NSLocale.currentLocale
    }
    return formatter.stringFromDate(date)
}

actual fun formatMonthYear(timestamp: Long): String {
    val date = NSDate(timeIntervalSinceReferenceDate = (timestamp / 1000.0) - 978307200.0)
    val formatter = NSDateFormatter().apply {
        dateFormat = "MMMM"
        locale = NSLocale("es_ES")
    }
    return formatter.stringFromDate(date).replaceFirstChar { it.uppercase() }
}
actual val platformRendersEmoji: Boolean = true

actual val isWebPlatform: Boolean = false
