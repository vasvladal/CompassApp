package com.example.compassapp.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectZero
import platform.UIKit.UIViewAutoresizingFlexibleHeight
import platform.UIKit.UIViewAutoresizingFlexibleWidth
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.WebKit.javaScriptEnabled

@OptIn(ExperimentalComposeUiApi::class, ExperimentalForeignApi::class)
@Composable
actual fun MapView(
    latitude: Double,
    longitude: Double,
    modifier: Modifier
) {
    val htmlContent = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.css" />
            <script src="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.js"></script>
            <style>
                html, body { margin: 0; padding: 0; height: 100%; width: 100%; background: #e5e5e5; }
                #map { height: 100%; width: 100%; }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                document.addEventListener('DOMContentLoaded', function() {
                    var map = L.map('map').setView([$latitude, $longitude], 15);
                    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                        attribution: '&copy; OpenStreetMap'
                    }).addTo(map);
                    L.marker([$latitude, $longitude]).addTo(map);
                });
            </script>
        </body>
        </html>
    """.trimIndent()

    UIKitView(
        factory = {
            val config = WKWebViewConfiguration()
            config.preferences.javaScriptEnabled = true
            val apply = WKWebView(frame = CGRectZero, configuration = config).apply {
                autoresizingMask =
                    UIViewAutoresizingFlexibleWidth or UIViewAutoresizingFlexibleHeight
            }
            apply
        },
        modifier = modifier,
        update = { webView ->
            webView.loadHTMLString(htmlContent, null)
        }
    )
}