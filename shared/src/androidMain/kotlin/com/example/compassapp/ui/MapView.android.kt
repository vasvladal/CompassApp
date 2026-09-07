package com.example.compassapp.ui

import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import java.io.InputStream

@Composable
actual fun MapView(
    latitude: Double,
    longitude: Double,
    modifier: Modifier
) {
    // Create HTML with a loading indicator and better error handling
    val htmlContent = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.css" />
            <script src="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.js"></script>
            <style>
                html, body { 
                    margin: 0; 
                    padding: 0; 
                    height: 100%; 
                    width: 100%; 
                    background: #e5e5e5; 
                }
                #map { 
                    height: 100%; 
                    width: 100%; 
                }
                #loading {
                    position: absolute;
                    top: 50%;
                    left: 50%;
                    transform: translate(-50%, -50%);
                    font-family: Arial, sans-serif;
                    color: #666;
                }
            </style>
        </head>
        <body>
            <div id="loading">Loading map...</div>
            <div id="map"></div>
            <script>
                (function() {
                    console.log('Map script starting...');
                    
                    function initMap() {
                        try {
                            var container = document.getElementById('map');
                            if (!container) {
                                console.error('Map container not found');
                                return;
                            }
                            
                            console.log('Creating map with coords: $latitude, $longitude');
                            
                            var map = L.map('map', {
                                center: [$latitude, $longitude],
                                zoom: 15,
                                zoomControl: true
                            });
                            
                            L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                                attribution: '&copy; OpenStreetMap contributors',
                                maxZoom: 19
                            }).addTo(map);
                            
                            L.marker([$latitude, $longitude]).addTo(map);
                            
                            // Remove loading indicator
                            var loading = document.getElementById('loading');
                            if (loading) loading.style.display = 'none';
                            
                            // Force resize
                            setTimeout(function() {
                                map.invalidateSize();
                            }, 100);
                            
                            console.log('Map initialized successfully');
                        } catch(e) {
                            console.error('Error initializing map:', e);
                        }
                    }
                    
                    // Initialize when DOM is ready
                    if (document.readyState === 'loading') {
                        document.addEventListener('DOMContentLoaded', initMap);
                    } else {
                        initMap();
                    }
                })();
            </script>
        </body>
        </html>
    """.trimIndent()

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                settings.cacheMode = android.webkit.WebSettings.LOAD_DEFAULT

                // Enable debugging
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                    WebView.setWebContentsDebuggingEnabled(true)
                }

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                        // Don't override URL loading
                        return false
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        // Ensure map is properly sized
                        view?.loadUrl("javascript:(function() { if (typeof map !== 'undefined') { map.invalidateSize(); } })();")
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage): Boolean {
                        android.util.Log.d("MapWebView", "${consoleMessage.message()} (${consoleMessage.lineNumber()})")
                        return true
                    }
                }
            }
        },
        modifier = modifier,
        update = { webView ->
            webView.loadDataWithBaseURL(
                "https://www.openstreetmap.org/",
                htmlContent,
                "text/html",
                "UTF-8",
                null
            )
        }
    )
}