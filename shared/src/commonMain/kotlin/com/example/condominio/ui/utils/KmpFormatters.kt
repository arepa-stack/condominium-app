package com.example.condominio.ui.utils

expect fun formatCurrency(amount: Double): String

expect fun formatDate(timestamp: Long, formatString: String = "MMM dd, yyyy"): String

expect fun formatMonthYear(timestamp: Long): String
