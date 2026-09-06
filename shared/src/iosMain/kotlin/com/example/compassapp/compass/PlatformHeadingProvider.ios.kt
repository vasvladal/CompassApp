package com.example.compassapp.compass

import platform.CoreLocation.CLHeading
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreMotion.CMMotionManager
import platform.Foundation.NSError
import platform.Foundation.NSOperationQueue
import platform.darwin.NSObject
import kotlin.math.PI

actual class PlatformHeadingProvider actual constructor(
    private val onHeadingUpdate: (degrees: Float, accuracyDegrees: Float?, magneticStrengthMicroTesla: Float?, pitch: Float, roll: Float) -> Unit
) {
    private val locationManager = CLLocationManager()
    private var delegate: HeadingDelegate? = null
    private var isRunning = false
    private val motionManager = CMMotionManager()
    private var latestPitch = 0f
    private var latestRoll = 0f
    private var lastHeading: Float? = null
    private var lastAccuracy: Float? = null

    actual fun start(onError: (String) -> Unit) {
        if (isRunning) return
        if (!CLLocationManager.headingAvailable()) { onError("Compass sensor is not available on this device"); return }
        val newDelegate = HeadingDelegate(this)
        delegate = newDelegate
        locationManager.delegate = newDelegate
        locationManager.requestWhenInUseAuthorization()
        locationManager.startUpdatingHeading()

        if (motionManager.isDeviceMotionAvailable) {
            motionManager.deviceMotionUpdateInterval = 0.1
            motionManager.startDeviceMotionUpdatesToQueue(NSOperationQueue.currentQueue ?: NSOperationQueue.mainQueue) { motion, error ->
                if (motion != null) {
                    latestPitch = (motion.attitude.pitch * 180.0 / PI).toFloat()
                    latestRoll = (motion.attitude.roll * 180.0 / PI).toFloat()
                    if (lastHeading != null) onHeadingUpdate(lastHeading!!, lastAccuracy, null, latestPitch, latestRoll)
                }
            }
        }
        isRunning = true
    }

    actual fun stop() {
        if (!isRunning) return
        locationManager.stopUpdatingHeading()
        locationManager.delegate = null
        delegate = null
        motionManager.stopDeviceMotionUpdates()
        isRunning = false
    }

    fun updateHeading(heading: Float, accuracy: Float?) {
        lastHeading = heading; lastAccuracy = accuracy
        onHeadingUpdate(heading, accuracy, null, latestPitch, latestRoll)
    }
}

private class HeadingDelegate(private val provider: PlatformHeadingProvider) : NSObject(), CLLocationManagerDelegateProtocol {
    override fun locationManager(manager: CLLocationManager, didUpdateHeading: CLHeading) {
        val trueHeading = didUpdateHeading.trueHeading
        val magneticHeading = didUpdateHeading.magneticHeading
        val heading = if (trueHeading >= 0) trueHeading else magneticHeading
        if (heading >= 0) provider.updateHeading(heading.toFloat(), didUpdateHeading.headingAccuracy.toFloat())
    }
    override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {}
}