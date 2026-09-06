package com.example.compassapp.compass

import kotlin.math.pow
import kotlin.math.round

/**
 * Device heading (compass) reading, in degrees from magnetic/true north.
 *
 * NOTE: dev.jordond.compass only covers geolocation & geocoding, it has no
 * magnetometer/heading API -- so heading is read directly from the platform
 * sensors via [PlatformHeadingProvider] below.
 */
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
            return HeadingData(
                degrees = normalized,
                cardinalDirection = directions[index],
                accuracyDegrees = accuracyDegrees,
                magneticStrengthMicroTesla = magneticStrengthMicroTesla
            )
        }
    }
}

/**
 * Rounds a Double to [decimals] places using only multiplatform-safe stdlib
 * functions (kotlin.math.*, no java.lang.Math / String.format in commonMain).
 */
fun Double.roundTo(decimals: Int): Double {
    val factor = 10.0.pow(decimals)
    return round(this * factor) / factor
}

/**
 * Platform-specific heading sensor access.
 *  - Android: SensorManager (accelerometer + magnetometer -> rotation matrix)
 *  - iOS: CLLocationManager.startUpdatingHeading()
 */
expect class PlatformHeadingProvider(
    onHeadingUpdate: (degrees: Float, accuracyDegrees: Float?, magneticStrengthMicroTesla: Float?) -> Unit
) {
    fun start(onError: (String) -> Unit)
    fun stop()
}
