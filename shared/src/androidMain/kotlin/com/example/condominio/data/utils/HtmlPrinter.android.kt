package com.example.condominio.data.utils

import android.content.Context
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberHtmlPrinter(): (title: String, html: String) -> Unit {
    val context = LocalContext.current
    // Held until printing starts: a WebView collected before onPageFinished never prints.
    val holder = remember { arrayOfNulls<WebView>(1) }
    return remember(context) {
        { title, html ->
            val webView = WebView(context)
            holder[0] = webView
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String?) {
                    val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                    printManager.print(title, view.createPrintDocumentAdapter(title), null)
                    holder[0] = null
                }
            }
            webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
        }
    }
}
