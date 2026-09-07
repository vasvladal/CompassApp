package com.example.compassapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSURL
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKNavigation
import platform.darwin.NSObject

private const val BROWSER_USER_AGENT =
    "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) " +
            "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1"

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun MapView(
    latitude: Double,
    longitude: Double,
    modifier: Modifier
) {
    val html = remember(latitude, longitude) { buildMapHtml(latitude, longitude) }
    var isLoading by remember { mutableStateOf(true) }
    val delegate = remember { MapNavDelegate { isLoading = false } }

    Box(modifier = modifier) {
        UIKitView(
            modifier = Modifier.matchParentSize(),
            factory = {
                val config = WKWebViewConfiguration()
                // FIX "access blocked": pretend to be a real browser
                config.applicationNameForUserAgent = BROWSER_USER_AGENT

                val webView = WKWebView(
                    frame = CGRectMake(0.0, 0.0, 0.0, 0.0),
                    configuration = config
                )
                webView.setOpaque(false)
                webView.navigationDelegate = delegate

                webView.loadHTMLString(
                    html,
                    baseURL = NSURL(string = "https://www.openstreetmap.org/")
                )
                webView
            },
            update = { webView ->
                val currentUrl = webView.URL?.absoluteString
                if (currentUrl == null || currentUrl == "about:blank") {
                    isLoading = true
                    webView.loadHTMLString(
                        html,
                        baseURL = NSURL(string = "https://www.openstreetmap.org/")
                    )
                }
            }
        )

        if (isLoading) {
            Box(
                modifier = Modifier.matchParentSize().background(Color(0xFF1A1A2E)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color(0xFFD0B7FF))
                    Text(
                        text = "Loading map…",
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
        }
    }
}

private class MapNavDelegate(
    private val onDone: () -> Unit
) : NSObject(), WKNavigationDelegateProtocol {
    override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
        onDone()
    }
}