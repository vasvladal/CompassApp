package com.example.compassapp.compass

/**
 * Returns the magnetic declination (in degrees) for a given location.
 * Positive value means East declination, Negative means West.
 * True Heading = Magnetic Heading + Declination.
 */
expect fun getMagneticDeclination(latitude: Double, longitude: Double, altitude: Float): Float