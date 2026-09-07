package com.example.compassapp.settings

fun aboutHtml(language: Language): String {
    val body = when (language) {
        Language.ENGLISH -> """
            <h1>CompassApp 1.0</h1>
            <p>A Kotlin Multiplatform compass with true-direction heading, GPS coordinates in DMS format, and a free OpenStreetMap view.</p>
            <h2>Features</h2>
            <ul>
              <li>True-north compass with magnetic declination correction</li>
              <li>Latitude / longitude in degrees, minutes, seconds</li>
              <li>OpenStreetMap WebView map (no API key required)</li>
              <li>Calibration wizard and bubble level</li>
            </ul>
            <h2>Credits</h2>
            <ul>
              <li>Map data &copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors</li>
              <li>Geolocation &amp; geocoding: <a href="https://compass.jordond.dev">Compass</a> library</li>
            </ul>
            <p class="muted">Version 1.0 (build 1) &middot; &copy; 2026 Example Corp</p>
        """.trimIndent()
        Language.RUSSIAN -> """
            <h1>CompassApp 1.0</h1>
            <p>Компас на Kotlin Multiplatform: истинное направление, координаты в формате ГМС и бесплатная карта OpenStreetMap.</p>
            <h2>Возможности</h2>
            <ul>
              <li>Компас с поправкой магнитного склонения (истинный север)</li>
              <li>Широта и долгота в градусах, минутах, секундах</li>
              <li>Карта OpenStreetMap в WebView (без API-ключа)</li>
              <li>Калибровка и пузырьковый уровень</li>
            </ul>
            <h2>Благодарности</h2>
            <ul>
              <li>Данные карты &copy; участники <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a></li>
              <li>Геолокация и геокодинг: библиотека <a href="https://compass.jordond.dev">Compass</a></li>
            </ul>
            <p class="muted">Версия 1.0 (сборка 1) &middot; &copy; 2026 Example Corp</p>
        """.trimIndent()
    }

    return """
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8"/>
<meta name="viewport" content="width=device-width, initial-scale=1.0"/>
<style>
  body{background:#000000;color:#F4F1FA;font-family:sans-serif;padding:16px;line-height:1.5;margin:0;}
  h1{color:#D0B7FF;font-size:20px;margin:0 0 8px 0;}
  h2{color:#D0B7FF;font-size:16px;margin:18px 0 6px 0;}
  a{color:#D0B7FF;}
  ul{padding-left:20px;}
  .muted{color:#888888;font-size:12px;margin-top:20px;}
</style>
</head>
<body>
$body
</body>
</html>
""".trimIndent()
}