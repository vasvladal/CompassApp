package com.example.compassapp.compass

actual fun getMagneticDeclination(latitude: Double, longitude: Double, altitude: Float): Float {
    // iOS CLHeading already provides trueHeading when location services are active.
    // We return 0f here because the iOS PlatformHeadingProvider already handles it.
    return 0f
}