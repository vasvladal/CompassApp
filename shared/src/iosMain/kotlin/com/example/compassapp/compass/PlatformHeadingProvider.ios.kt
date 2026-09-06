package com.example.compassapp.compass

import platform.CoreLocation.CLHeading
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.Foundation.NSError
import platform.darwin.NSObject

actual class PlatformHeadingProvider actual constructor(
    private val onHeadingUpdate: (degrees: Float, accuracyDegrees: Float?) -> Unit
) {
    private val locationManager = CLLocationManager()
    private var delegate: HeadingDelegate? = null
    private var isRunning = false

    actual fun start(onError: (String) -> Unit) {
        if (isRunning) return

        if (!CLLocationManager.headingAvailable()) {
            onError("Compass sensor is not available on this device")
            return
        }

        val newDelegate = HeadingDelegate(onHeadingUpdate)
        delegate = newDelegate
        locationManager.delegate = newDelegate

        // Heading updates require location authorization on iOS.
        locationManager.requestWhenInUseAuthorization()
        locationManager.startUpdatingHeading()
        isRunning = true
    }

    actual fun stop() {
        if (!isRunning) return
        locationManager.stopUpdatingHeading()
        locationManager.delegate = null
        delegate = null
        isRunning = false
    }
}

private class HeadingDelegate(
    private val onHeadingUpdate: (degrees: Float, accuracyDegrees: Float?) -> Unit
) : NSObject(), CLLocationManagerDelegateProtocol {

    override fun locationManager(manager: CLLocationManager, didUpdateHeading: CLHeading) {
        val trueHeading = didUpdateHeading.trueHeading
        val magneticHeading = didUpdateHeading.magneticHeading
        val heading = if (trueHeading >= 0) trueHeading else magneticHeading
        if (heading >= 0) {
            onHeadingUpdate(heading.toFloat(), didUpdateHeading.headingAccuracy.toFloat())
        }
    }

    override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
        // Heading errors surface as no further updates; startTracking() on the
        // Geolocator side already reports location-permission problems.
    }
}
