package com.example.compassapp.compass

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import android.view.WindowManager
import kotlin.math.PI

actual class PlatformHeadingProvider actual constructor(
    private val onHeadingUpdate: (degrees: Float, accuracyDegrees: Float?, magneticStrengthMicroTesla: Float?) -> Unit
) {
    private val sensorManager: SensorManager by lazy {
        AndroidContextHolder.appContext.getSystemService(SensorManager::class.java)
    }

    private val windowManager: WindowManager by lazy {
        AndroidContextHolder.appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    // Fallback path only, used on the rare device with no rotation-vector sensor.
    private val gravity = FloatArray(3)
    private val geomagnetic = FloatArray(3)
    private var hasGravity = false
    private var hasGeomagnetic = false

    private val rotationMatrix = FloatArray(9)
    private val remappedMatrix = FloatArray(9)
    private val orientation = FloatArray(3)

    private var isRunning = false
    private var usingRotationVector = false
    private var smoothedDegrees: Float? = null
    private var magneticStrengthMicroTesla: Float? = null

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            when (event.sensor.type) {
                Sensor.TYPE_ROTATION_VECTOR -> {
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    publishAzimuth(rotationMatrix, accuracyDegrees(event.values))
                }
                Sensor.TYPE_ACCELEROMETER -> {
                    if (usingRotationVector) return
                    System.arraycopy(event.values, 0, gravity, 0, gravity.size)
                    hasGravity = true
                    publishFromAccelMag()
                }
                Sensor.TYPE_MAGNETIC_FIELD -> {
                    magneticStrengthMicroTesla = kotlin.math.sqrt(
                        event.values.getOrElse(0) { 0f } * event.values.getOrElse(0) { 0f } +
                        event.values.getOrElse(1) { 0f } * event.values.getOrElse(1) { 0f } +
                        event.values.getOrElse(2) { 0f } * event.values.getOrElse(2) { 0f }
                    )
                    if (usingRotationVector) return
                    System.arraycopy(event.values, 0, geomagnetic, 0, geomagnetic.size)
                    hasGeomagnetic = true
                    publishFromAccelMag()
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    private fun publishFromAccelMag() {
        if (!hasGravity || !hasGeomagnetic) return
        if (SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)) {
            publishAzimuth(rotationMatrix, accuracyDegrees = null)
        }
    }

    /**
     * Remaps the rotation matrix for the device's current display rotation before
     * reading the azimuth.
     *
     * Sensor axes are fixed to the device's natural (sensor-default) orientation,
     * not to whatever orientation the screen is currently showing. Without this
     * remap step the reading is only correct in that one natural orientation --
     * on phones held/rotated differently, and especially on tablets (whose
     * natural orientation is landscape), this shows up as a fixed 90/180/270°
     * offset, which is why facing true north could never read close to 0°.
     */
    private fun publishAzimuth(matrix: FloatArray, accuracyDegrees: Float?) {
        val (axisX, axisY) = when (currentRotation()) {
            Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
            Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
            Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
            else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
        }
        SensorManager.remapCoordinateSystem(matrix, axisX, axisY, remappedMatrix)
        SensorManager.getOrientation(remappedMatrix, orientation)

        val rawDegrees = (orientation[0] * 180f / PI.toFloat() + 360f) % 360f
        onHeadingUpdate(lowPass(rawDegrees), accuracyDegrees, magneticStrengthMicroTesla)
    }

    /**
     * Circular low-pass filter so small sensor jitter doesn't make the needle
     * twitch, while still snapping to a real heading change quickly. Handles
     * the 0/360° wrap-around correctly.
     */
    private fun lowPass(newDegrees: Float, factor: Float = 0.2f): Float {
        val previous = smoothedDegrees ?: run {
            smoothedDegrees = newDegrees
            return newDegrees
        }
        val delta = (newDegrees - previous + 540f) % 360f - 180f
        val result = (previous + factor * delta + 360f) % 360f
        smoothedDegrees = result
        return result
    }

    /** values[4] carries estimated heading accuracy (radians) on devices that report it. */
    private fun accuracyDegrees(values: FloatArray): Float? =
        if (values.size >= 5) values[4] * 180f / PI.toFloat() else null

    @Suppress("DEPRECATION")
    private fun currentRotation(): Int = windowManager.defaultDisplay.rotation

    actual fun start(onError: (String) -> Unit) {
        if (isRunning) return
        smoothedDegrees = null

        val rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (rotationVector != null) {
            usingRotationVector = true
            isRunning = true
            sensorManager.registerListener(listener, rotationVector, SensorManager.SENSOR_DELAY_UI)
            sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.let {
                sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
            }
            return
        }

        // Fallback for the rare device without a rotation-vector sensor.
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        if (accelerometer == null || magnetometer == null) {
            onError("Compass sensors are not available on this device")
            return
        }

        usingRotationVector = false
        isRunning = true
        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(listener, magnetometer, SensorManager.SENSOR_DELAY_UI)
    }

    actual fun stop() {
        if (!isRunning) return
        sensorManager.unregisterListener(listener)
        hasGravity = false
        hasGeomagnetic = false
        smoothedDegrees = null
        magneticStrengthMicroTesla = null
        isRunning = false
    }
}
