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

    private fun publishAzimuth(matrix: FloatArray, accuracyDegrees: Float?) {
        // Get the current display rotation
        val rotation = currentRotation()

        // Remap the coordinate system based on the device's current orientation
        // This ensures the compass works correctly in both portrait and landscape
        val (axisX, axisY) = when (rotation) {
            Surface.ROTATION_0 -> SensorManager.AXIS_X to SensorManager.AXIS_Y
            Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
            Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
            Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
            else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
        }

        SensorManager.remapCoordinateSystem(matrix, axisX, axisY, remappedMatrix)
        SensorManager.getOrientation(remappedMatrix, orientation)

        // Get the azimuth (heading) from the orientation array
        // orientation[0] = azimuth (rotation around Z-axis)
        var rawDegrees = (orientation[0] * 180f / PI.toFloat() + 360f) % 360f

        // Apply low-pass filter to smooth the reading
        val filteredDegrees = lowPass(rawDegrees)

        onHeadingUpdate(filteredDegrees, accuracyDegrees, magneticStrengthMicroTesla)
    }

    /**
     * Circular low-pass filter so small sensor jitter doesn't make the needle twitch
     */
    private fun lowPass(newDegrees: Float, factor: Float = 0.15f): Float {
        val previous = smoothedDegrees ?: run {
            smoothedDegrees = newDegrees
            return newDegrees
        }
        var delta = newDegrees - previous
        // Handle wrap-around
        if (delta > 180f) delta -= 360f
        if (delta < -180f) delta += 360f
        val result = (previous + factor * delta + 360f) % 360f
        smoothedDegrees = result
        return result
    }

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

        // Fallback for devices without rotation vector sensor
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
