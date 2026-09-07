package com.example.compassapp.ui

import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
actual fun HtmlView(html: String, modifier: Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = false
                settings.domStorageEnabled = true
                setBackgroundColor(0xFF000000.toInt())
                tag = html
                loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            if (webView.tag as? String != html) {
                webView.tag = html
                webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
            }
        }
    )
}