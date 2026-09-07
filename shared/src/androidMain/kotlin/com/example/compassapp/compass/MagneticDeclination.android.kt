package com.example.compassapp.compass

import android.hardware.GeomagneticField

actual fun getMagneticDeclination(latitude: Double, longitude: Double, altitude: Float): Float {
    return try {
        val geomagneticField = GeomagneticField(
            latitude.toFloat(),
            longitude.toFloat(),
            altitude,
            System.currentTimeMillis()
        )
        geomagneticField.declination
    } catch (e: Exception) {
        0f
    }
}