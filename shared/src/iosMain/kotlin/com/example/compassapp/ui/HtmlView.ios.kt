package com.example.compassapp.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectMake
import platform.WebKit.WKWebView

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun HtmlView(html: String, modifier: Modifier) {
    var loadedHtml by remember { mutableStateOf(html) }
    UIKitView(
        modifier = modifier,
        factory = {
            val webView = WKWebView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0))
            webView.setOpaque(false)
            webView.loadHTMLString(html, baseURL = null)
            webView
        },
        update = { webView ->
            if (loadedHtml != html) {
                loadedHtml = html
                webView.loadHTMLString(html, baseURL = null)
            }
        }
    )
}