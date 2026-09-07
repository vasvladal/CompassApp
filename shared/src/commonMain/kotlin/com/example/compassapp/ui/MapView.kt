package com.example.compassapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.compassapp.compass.roundTo // <-- Added import for your project's extension function
import kotlin.math.abs

@Composable
expect fun MapView(
    latitude: Double,
    longitude: Double,
    modifier: Modifier = Modifier
)

/**
 * Displays coordinates in Degrees, Minutes, Seconds format.
 */
@Composable
fun CoordinatesDisplay(
    latitude: Double,
    longitude: Double
) {
    val (latDeg, latMin, latSec) = decimalToDms(latitude)
    val (lonDeg, lonMin, lonSec) = decimalToDms(longitude)
    val latDir = if (latitude >= 0) "N" else "S"
    val lonDir = if (longitude >= 0) "E" else "W"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A2E))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Lat: ${formatDms(latDeg, latMin, latSec)} $latDir",
            color = Color(0xFFD0B7FF),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "Lng: ${formatDms(lonDeg, lonMin, lonSec)} $lonDir",
            color = Color(0xFFD0B7FF),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun decimalToDms(value: Double): Triple<Int, Int, Double> {
    val totalSeconds = abs(value) * 3600.0
    var degrees = (totalSeconds / 3600.0).toInt()
    var minutes = ((totalSeconds - degrees * 3600.0) / 60.0).toInt()
    var seconds = (totalSeconds - degrees * 3600.0 - minutes * 60.0).roundTo(2)

    // Carry-over when rounding pushes seconds to 60
    if (seconds >= 60.0) { seconds -= 60.0; minutes += 1 }
    if (minutes >= 60)   { minutes -= 60; degrees += 1 }

    return Triple(degrees, minutes, seconds)
}

private fun formatDms(degrees: Int, minutes: Int, seconds: Double): String {
    // FIX: Replaced .round with your project's .roundTo(2) extension function
    return "${degrees}°${minutes.toString().padStart(2, '0')}'${seconds.roundTo(2).toString().padStart(5, '0')}\""
}