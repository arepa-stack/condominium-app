package com.example.condominio.ui.utils

import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterCurrencyStyle
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale

actual fun formatCurrency(amount: Double): String {
    val formatter = NSNumberFormatter().apply {
        numberStyle = NSNumberFormatterCurrencyStyle
    }
    return formatter.stringFromNumber(NSNumber(amount))?.replace(Regex("[^0-9.,]"), "") ?: amount.toString()
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
