package com.example.condominio.data.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

// Opened synchronously from the click, so popup blockers allow it; the tab closes itself after printing.
private fun printInNewTab(html: String) {
    js("""
        const w = window.open('', '_blank');
        if (w) {
            w.document.open();
            w.document.write(html);
            w.document.close();
            w.onafterprint = () => w.close();
            w.focus();
            setTimeout(() => w.print(), 300);
        }
    """)
}

@Composable
actual fun rememberHtmlPrinter(): (title: String, html: String) -> Unit =
    remember { { _, html -> printInNewTab(html) } }
