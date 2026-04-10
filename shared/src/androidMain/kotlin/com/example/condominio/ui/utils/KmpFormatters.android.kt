package com.example.condominio.ui.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

actual fun formatCurrency(amount: Double): String {
    return String.format(Locale.US, "%.2f", amount)
}

actual fun formatDate(timestamp: Long, formatString: String): String {
    val date = Date(timestamp)
    val formatter = SimpleDateFormat(formatString, Locale.getDefault())
    return formatter.format(date)
}

actual fun formatMonthYear(timestamp: Long): String {
    val date = Date(timestamp)
    val formatter = SimpleDateFormat("MMMM", Locale.forLanguageTag("es-ES"))
    return formatter.format(date).replaceFirstChar { it.uppercase() }
}
