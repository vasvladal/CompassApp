package com.example.compassapp.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class Language(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    RUSSIAN("ru", "Русский");

    companion object {
        fun fromCode(code: String?): Language =
            entries.firstOrNull { it.code == code } ?: ENGLISH
    }
}

data class AppStrings(
    val trueDirection: String, val locating: String, val locationDetails: String,
    val latitude: String, val longitude: String, val decimal: String,
    val altitude: String, val accuracy: String, val speed: String,
    val refreshLocation: String, val showMap: String, val mapView: String,
    val close: String, val strength: String, val calibrateCompass: String,
    val rotateFigure8: String, val cancel: String, val settings: String,
    val about: String, val language: String, val languageHint: String
)

fun stringsFor(language: Language): AppStrings = when (language) {
    Language.ENGLISH -> AppStrings(
        "TRUE DIRECTION", "Locating…", "Location Details",
        "Latitude", "Longitude", "decimal",
        "Altitude", "Accuracy", "Speed",
        "Refresh Location", "Show Map", "Map View",
        "Close", "STRENGTH", "Calibrate Compass",
        "Rotate your device in a figure-8 motion", "Cancel", "Settings",
        "About", "Language",
        "The interface language is applied immediately and saved on this device."
    )
    Language.RUSSIAN -> AppStrings(
        "ИСТИННОЕ НАПРАВЛЕНИЕ", "Определение местоположения…", "Данные местоположения",
        "Широта", "Долгота", "десятичные",
        "Высота", "Точность", "Скорость",
        "Обновить позицию", "Показать карту", "Карта",
        "Закрыть", "СИЛА ПОЛЯ", "Калибровка компаса",
        "Вращайте устройство восьмёркой", "Отмена", "Настройки",
        "О приложении", "Язык",
        "Язык интерфейса применяется сразу и сохраняется на устройстве."
    )
}

object AppSettings {
    private val _language = MutableStateFlow(Language.fromCode(loadLanguageCode()))
    val language: StateFlow<Language> = _language.asStateFlow()

    fun setLanguage(language: Language) {
        saveLanguageCode(language.code)
        _language.value = language
    }
}