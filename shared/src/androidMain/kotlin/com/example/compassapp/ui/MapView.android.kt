package com.example.compassapp.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

private const val RED_PIN_JS = """
(function() {
    var redPin = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='32' height='44' viewBox='0 0 32 44'%3E%3Cpath d='M16 0C7.2 0 0 7.2 0 16c0 12 16 28 16 28s16-16 16-28C32 7.2 24.8 0 16 0z' fill='%23FF0000' stroke='%23FFFFFF' stroke-width='2'/%3E%3Ccircle cx='16' cy='16' r='6' fill='%23FFFFFF'/%3E%3C/svg%3E";
    function replace() {
        var imgs = document.querySelectorAll('.leaflet-marker-pane img, .leaflet-marker-icon');
        for (var i = 0; i < imgs.length; i++) {
            if (imgs[i].src !== redPin) {
                imgs[i].src = redPin;
                imgs[i].style.width = '32px';
                imgs[i].style.height = '44px';
                imgs[i].style.marginLeft = '-16px';
                imgs[i].style.marginTop = '-44px';
            }
        }
        var shadows = document.querySelectorAll('.leaflet-marker-shadow, .leaflet-shadow-pane img');
        for (var j = 0; j < shadows.length; j++) {
            shadows[j].style.display = 'none';
        }
    }
    replace();
    setInterval(replace, 300);
})()
"""

@SuppressLint("SetJavaScriptEnabled")
@Composable
actual fun MapView(
    latitude: Double,
    longitude: Double,
    modifier: Modifier
) {
    val url = remember(latitude, longitude) { buildOsmEmbedUrl(latitude, longitude) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.matchParentSize(),
            factory = { context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    settings.setSupportZoom(true)
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = false

                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            isLoading = true
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isLoading = false

                            // NO "javascript:" prefix — evaluateJavascript takes raw JS
                            view?.evaluateJavascript(RED_PIN_JS, null)

                            // Retry after delay in case Leaflet loads marker later
                            Handler(Looper.getMainLooper()).postDelayed({
                                view?.evaluateJavascript(RED_PIN_JS, null)
                            }, 2000)
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            if (request?.isForMainFrame == true) {
                                isLoading = false
                                hasError = true
                            }
                        }
                    }

                    webChromeClient = WebChromeClient()
                    setBackgroundColor(0xFF1A1A2E.toInt())
                    loadUrl(url)
                }
            },
            update = { webView ->
                if (webView.url != url) {
                    isLoading = true
                    hasError = false
                    webView.loadUrl(url)
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

        if (hasError) {
            Box(
                modifier = Modifier.matchParentSize().background(Color(0xFF1A1A2E)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Failed to load map.\nCheck internet connection.",
                    color = Color(0xFFFF5A60),
                    fontSize = 14.sp
                )
            }
        }
    }
}