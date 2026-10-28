package com.example.condominio.data.utils

import androidx.compose.runtime.Composable

// ponytail: no-op like the external viewer; UIPrintInteractionController with a UIMarkupTextPrintFormatter when iOS ships.
@Composable
actual fun rememberHtmlPrinter(): (title: String, html: String) -> Unit = { _, _ -> }
