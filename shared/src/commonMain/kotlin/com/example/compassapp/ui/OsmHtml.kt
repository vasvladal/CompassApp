package com.example.compassapp.ui

fun buildOsmEmbedUrl(latitude: Double, longitude: Double): String {
    val delta = 0.005
    val left = longitude - delta
    val bottom = latitude - delta
    val right = longitude + delta
    val top = latitude + delta

    return "https://www.openstreetmap.org/export/embed.html" +
            "?bbox=$left,$bottom,$right,$top" +
            "&layer=mapnik" +
            "&marker=$latitude,$longitude" // Restored so the map handles the pin
}