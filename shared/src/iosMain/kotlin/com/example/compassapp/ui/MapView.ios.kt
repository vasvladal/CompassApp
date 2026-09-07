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
import platform.Foundation.NSURLRequest
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKNavigation
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun MapView(
    latitude: Double,
    longitude: Double,
    modifier: Modifier
) {
    val url = remember(latitude, longitude) { buildOsmEmbedUrl(latitude, longitude) }
    var isLoading by remember { mutableStateOf(true) }

    // Delegate to inject CSS once the map DOM is ready
    val delegate = remember {
        MapNavigationDelegate { isLoading = false }
    }

    Box(modifier = modifier) {
        UIKitView(
            modifier = Modifier.matchParentSize(),
            factory = {
                val config = WKWebViewConfiguration()
                val webView = WKWebView(
                    frame = CGRectMake(0.0, 0.0, 0.0, 0.0),
                    configuration = config
                )
                webView.setOpaque(false)
                webView.navigationDelegate = delegate

                val nsUrl = NSURL(string = url)
                val request = NSURLRequest(uRL = nsUrl)
                webView.loadRequest(request)
                webView
            },
            update = { webView ->
                val currentUrl = webView.URL?.absoluteString
                if (currentUrl != url) {
                    isLoading = true
                    val nsUrl = NSURL(string = url)
                    val request = NSURLRequest(uRL = nsUrl)
                    webView.loadRequest(request)
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

private class MapNavigationDelegate(
    private val onFinished: () -> Unit
) : NSObject(), WKNavigationDelegateProtocol {
    override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
        onFinished()

        // Inject CSS to recolor the default blue marker to glowing pink/red
        val jsSource = """
            var style = document.createElement('style');
            style.innerHTML = '.leaflet-marker-icon { filter: hue-rotate(140deg) saturate(5) brightness(1.2) drop-shadow(0 0 5px white) drop-shadow(0 0 10px #FF1744) !important; z-index: 9999 !important; } .leaflet-marker-shadow { display: none !important; }';
            document.head.appendChild(style);
        """.trimIndent()
        webView.evaluateJavaScript(jsSource, null)
    }
}