package com.example.compassapp.settings

import platform.Foundation.NSUserDefaults

private const val KEY_LANG = "language_code"

actual fun saveLanguageCode(code: String) {
    NSUserDefaults.standardUserDefaults.setObject(code, KEY_LANG)
}

actual fun loadLanguageCode(): String? =
    NSUserDefaults.standardUserDefaults.stringForKey(KEY_LANG)