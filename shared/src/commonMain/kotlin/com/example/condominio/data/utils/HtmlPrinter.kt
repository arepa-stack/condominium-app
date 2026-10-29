package com.example.condominio.data.utils

import androidx.compose.runtime.Composable

/**
 * Opens the system print dialog for a full HTML page; "Save as PDF" there is the download.
 * Web prints it from a new tab, Android through PrintManager; iOS is not wired yet (no-op).
 */
@Composable
expect fun rememberHtmlPrinter(): (title: String, html: String) -> Unit
