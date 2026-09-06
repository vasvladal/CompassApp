package com.example.compassapp.compass

import kotlin.math.pow
import kotlin.math.round

data class HeadingData(
    val degrees: Float,
    val cardinalDirection: String,
    val accuracyDegrees: Float? = null,
    val magneticStrengthMicroTesla: Float? = null
) {
    fun formatDegrees(): String = "${degrees.toDouble().roundTo(1)}°"
    companion object {
        private val directions = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        fun fromDegrees(degrees: Float, accuracyDegrees: Float? = null, magneticStrengthMicroTesla: Float? = null): HeadingData {
            val normalized = ((degrees % 360) + 360) % 360
            val index = ((normalized + 22.5f) / 45f).toInt() % 8
            return HeadingData(normalized, directions[index], accuracyDegrees, magneticStrengthMicroTesla)
        }
    }
}

fun Double.roundTo(decimals: Int): Double {
    val factor = 10.0.pow(decimals)
    return round(this * factor) / factor
}

expect class PlatformHeadingProvider(
    onHeadingUpdate: (degrees: Float, accuracyDegrees: Float?, magneticStrengthMicroTesla: Float?, pitch: Float, roll: Float) -> Unit
) {
    fun start(onError: (String) -> Unit)
    fun stop()
}