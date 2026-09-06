package com.example.compassapp.compass

import android.content.Context
import androidx.startup.Initializer

/**
 * Holds the application Context so common/androidMain code (like the
 * heading sensor provider) can reach it without every caller having to
 * thread a Context through commonMain APIs.
 *
 * Populated automatically at process start by [CompassContextInitializer]
 * via androidx.startup -- see shared/src/androidMain/AndroidManifest.xml.
 */
internal object AndroidContextHolder {
    lateinit var appContext: Context
        private set

    fun init(context: Context) {
        appContext = context.applicationContext
    }
}

class CompassContextInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        AndroidContextHolder.init(context)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
