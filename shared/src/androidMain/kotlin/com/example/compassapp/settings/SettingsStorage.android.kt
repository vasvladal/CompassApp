package com.example.compassapp.settings

import android.content.Context
import com.example.compassapp.compass.AndroidContextHolder

private const val PREFS = "compass_app_settings"
private const val KEY_LANG = "language_code"

actual fun saveLanguageCode(code: String) {
    AndroidContextHolder.appContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .edit().putString(KEY_LANG, code).apply()
}

actual fun loadLanguageCode(): String? =
    AndroidContextHolder.appContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getString(KEY_LANG, null)