package com.example.compassapp.compass

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.PI

actual class PlatformHeadingProvider actual constructor(
    private val onHeadingUpdate: (degrees: Float, accuracyDegrees: Float?) -> Unit
) {
    private val sensorManager: SensorManager by lazy {
        AndroidContextHolder.appContext.getSystemService(SensorManager::class.java)
    }

    private val gravity = FloatArray(3)
    private val geomagnetic = FloatArray(3)
    private var hasGravity = false
    private var hasGeomagnetic = false
    private var isRunning = false

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    System.arraycopy(event.values, 0, gravity, 0, gravity.size)
                    hasGravity = true
                }
                Sensor.TYPE_MAGNETIC_FIELD -> {
                    System.arraycopy(event.values, 0, geomagnetic, 0, geomagnetic.size)
                    hasGeomagnetic = true
                }
            }

            if (hasGravity && hasGeomagnetic) {
                val rotationMatrix = FloatArray(9)
                val success = SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)
                if (success) {
                    val orientation = FloatArray(3)
                    SensorManager.getOrientation(rotationMatrix, orientation)
                    val azimuthRadians = orientation[0]
                    val azimuthDegrees = (azimuthRadians * 180f / PI.toFloat() + 360f) % 360f
                    onHeadingUpdate(azimuthDegrees, null)
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    actual fun start(onError: (String) -> Unit) {
        if (isRunning) return

        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        if (accelerometer == null || magnetometer == null) {
            onError("Compass sensors are not available on this device")
            return
        }

        isRunning = true
        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(listener, magnetometer, SensorManager.SENSOR_DELAY_UI)
    }

    actual fun stop() {
        if (!isRunning) return
        sensorManager.unregisterListener(listener)
        hasGravity = false
        hasGeomagnetic = false
        isRunning = false
    }
}
