package com.example.compassapp.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.compassapp.compass.CompassRepository
import com.example.compassapp.compass.HeadingData
import com.example.compassapp.compass.roundTo
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.abs

private val Black = Color(0xFF000000)
private val White = Color(0xFFF4F1FA)
private val Purple = Color(0xFFD0B7FF)
private val PurpleText = Color(0xFF34224E)
private val Red = Color(0xFFFF0010)
private val DialGrey = Color(0xFF888888)
private val DialFace = Color(0xFF030303)
private val LightGray = Color(0xFF888888)
private val DarkGray = Color(0xFF444444)

private fun decimalToDMS(value: Double): Triple<Int, Int, Double> {
    val degrees = value.toInt()
    val minutesDecimal = abs((value - degrees) * 60)
    val minutes = minutesDecimal.toInt()
    val seconds = (minutesDecimal - minutes) * 60
    return Triple(abs(degrees), minutes, seconds)
}

private fun formatDMS(degrees: Int, minutes: Int, seconds: Double): String {
    return "${degrees}°${minutes.toString().padStart(2, '0')}'${seconds.roundTo(2).toString().padStart(5, '0')}\""
}

@Composable
fun CompassApp(repository: CompassRepository = remember { CompassRepository() }) {
    MaterialTheme(colorScheme = darkColorScheme(primary = Purple, background = Black, surface = Black, onBackground = White, onSurface = White)) {
        Box(modifier = Modifier.fillMaxSize().background(Black)) { CompassScreen(repository) }
    }
}

@Composable
private fun CompassScreen(repository: CompassRepository) {
    val location by repository.location.collectAsState()
    val locationError by repository.locationError.collectAsState()
    val placeName by repository.placeName.collectAsState()
    val heading by repository.heading.collectAsState()
    val headingError by repository.headingError.collectAsState()
    val pitch by repository.pitch.collectAsState()
    val roll by repository.roll.collectAsState()

    var showLocationDetails by remember { mutableStateOf(false) }
    var showCalibration by remember { mutableStateOf(false) }
    var calibrationProgress by remember { mutableStateOf(0f) }
    var lastCalibrationHeading by remember { mutableStateOf<Float?>(null) }
    var totalRotation by remember { mutableStateOf(0f) }
    var showMap by remember { mutableStateOf(false) }

    val rawHeading = heading?.degrees ?: 0f

    LaunchedEffect(showCalibration, rawHeading) {
        if (showCalibration) {
            val last = lastCalibrationHeading
            if (last != null) {
                var delta = rawHeading - last
                if (delta > 180f) delta -= 360f
                if (delta < -180f) delta += 360f
                totalRotation += abs(delta)
                calibrationProgress = (totalRotation / 720f).coerceIn(0f, 1f)
                if (calibrationProgress >= 1f) { showCalibration = false; calibrationProgress = 0f; totalRotation = 0f }
            }
            lastCalibrationHeading = rawHeading
        } else { lastCalibrationHeading = null; totalRotation = 0f; calibrationProgress = 0f }
    }

    DisposableEffect(Unit) { repository.startAll(); onDispose { repository.stopAll() } }

    Box(modifier = Modifier.fillMaxSize().background(Black)) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.weight(0.05f))

            Box(modifier = Modifier.height(32.dp).width(168.dp).clip(RoundedCornerShape(24.dp)).background(Purple), contentAlignment = Alignment.Center) {
                Text(text = "TRUE DIRECTION", color = PurpleText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(bottom = 4.dp)) {
                Text(text = rawHeading.toDouble().roundTo(0).toInt().toString(), color = White, fontSize = 42.sp, fontWeight = FontWeight.Normal)
                Text("°", modifier = Modifier.padding(bottom = 4.dp, start = 2.dp), color = White, fontSize = 24.sp)
                Spacer(Modifier.width(8.dp))
                Text(HeadingData.fromDegrees(rawHeading).cardinalDirection, modifier = Modifier.padding(bottom = 4.dp), color = White, fontSize = 28.sp, fontWeight = FontWeight.Normal)
            }
            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.clickable { showLocationDetails = !showLocationDetails }.padding(4.dp)) {
                Text(text = placeName ?: "Locating…", color = White, fontSize = 16.sp, fontWeight = FontWeight.Normal)
                Spacer(Modifier.width(6.dp))
                Text(text = "📍", fontSize = 14.sp)
            }

            if (showLocationDetails && location != null) {
                val lat = location!!.coordinates.latitude; val lon = location!!.coordinates.longitude
                val (latDeg, latMin, latSec) = decimalToDMS(lat); val (lonDeg, lonMin, lonSec) = decimalToDMS(lon)
                val latDir = if (lat >= 0) "N" else "S"; val lonDir = if (lon >= 0) "E" else "W"
                val altitude = location!!.mslAltitude; val accuracy = location!!.accuracy; val speed = location!!.speed

                Column(modifier = Modifier.padding(vertical = 8.dp).background(DarkGray.copy(alpha = 0.3f)).padding(12.dp).clip(RoundedCornerShape(8.dp)), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "📍 Location Details", color = Purple, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(text = "Latitude: ${formatDMS(latDeg, latMin, latSec)} $latDir", color = White, fontSize = 13.sp)
                    Text(text = "         ${abs(lat)}° (decimal)", color = LightGray, fontSize = 11.sp)
                    Spacer(Modifier.height(2.dp))
                    Text(text = "Longitude: ${formatDMS(lonDeg, lonMin, lonSec)} $lonDir", color = White, fontSize = 13.sp)
                    Text(text = "          ${abs(lon)}° (decimal)", color = LightGray, fontSize = 11.sp)
                    Spacer(Modifier.height(4.dp))
                    if (altitude != null) Text(text = "Altitude: ${(altitude as Double).roundTo(1)} m", color = LightGray, fontSize = 12.sp)
                    if (accuracy != null) Text(text = "Accuracy: ±${(accuracy as Double).roundTo(1)} m", color = LightGray, fontSize = 12.sp)
                    if (speed != null) Text(text = "Speed: ${(speed as Double * 3.6).roundTo(1)} km/h", color = LightGray, fontSize = 12.sp)
                    Button(onClick = { repository.refreshLocation() }, modifier = Modifier.padding(top = 4.dp)) { Text("Refresh Location", fontSize = 12.sp) }
                    Button(onClick = { showMap = true }, modifier = Modifier.padding(top = 4.dp)) { Text("Show Map", fontSize = 12.sp) }
                }
            }

            if (headingError != null) Text(headingError ?: "", color = Color(0xFFFF5A60), fontSize = 10.sp)
            if (locationError != null) Text(locationError ?: "", color = Color(0xFFFF5A60), fontSize = 10.sp)
            Spacer(Modifier.height(6.dp))

            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF383838)))
            Box(modifier = Modifier.height(18.dp).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                Canvas(Modifier.size(14.dp)) {
                    val p = Path().apply { moveTo(size.width / 2f, size.height); lineTo(0f, 0f); lineTo(size.width, 0f); close() }
                    drawPath(p, color = White)
                }
            }
            Spacer(Modifier.height(6.dp))

            Box(modifier = Modifier.fillMaxWidth().weight(0.5f), contentAlignment = Alignment.Center) {
                CircularCompass(heading = heading, pitch = pitch, roll = roll, modifier = Modifier.fillMaxSize())
            }

            Spacer(Modifier.height(6.dp))
            Text(text = "STRENGTH ${heading?.magneticStrengthMicroTesla?.let { "${it.toDouble().roundTo(0).toInt()} μT" } ?: "—"}", color = White, fontSize = 13.sp, modifier = Modifier.padding(bottom = 6.dp))
            Button(onClick = { showCalibration = true }, modifier = Modifier.padding(bottom = 8.dp)) { Text("Calibrate Compass", fontSize = 12.sp) }
            Spacer(Modifier.weight(0.05f))
        }

        if (showCalibration) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)).clickable { }) {
                Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Calibrate Compass", color = White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    Text("Rotate your device in a figure-8 motion", color = White, fontSize = 16.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
                    Spacer(Modifier.height(24.dp))
                    Box(modifier = Modifier.width(200.dp).height(8.dp).clip(RoundedCornerShape(4.dp)).background(DarkGray)) {
                        Box(modifier = Modifier.fillMaxWidth(calibrationProgress).height(8.dp).clip(RoundedCornerShape(4.dp)).background(Purple))
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("${(calibrationProgress * 100).toInt()}%", color = White, fontSize = 14.sp)
                    Spacer(Modifier.height(32.dp))
                    Button(onClick = { showCalibration = false }) { Text("Cancel") }
                }
            }
        }

        // Map Overlay
        if (showMap && location != null) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "📍 Map View", color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Button(onClick = { showMap = false }) { Text("Close") }
                    }

                    // Add a loading indicator while map loads
                    Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        MapView(
                            latitude = location!!.coordinates.latitude,
                            longitude = location!!.coordinates.longitude,
                            modifier = Modifier.fillMaxSize()
                        )
                        // Optional: Add a loading spinner overlay
                        // You can add a progress indicator here if needed
                    }

                    CoordinatesDisplay(
                        latitude = location!!.coordinates.latitude,
                        longitude = location!!.coordinates.longitude
                    )
                }
            }
        }
    }
}

@Composable
private fun CircularCompass(heading: HeadingData?, pitch: Float = 0f, roll: Float = 0f, modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()
    val target = heading?.degrees ?: 0f
    val animatedDegrees = remember { Animatable(0f) }

    LaunchedEffect(target) {
        val current = animatedDegrees.value
        var delta = (target - current) % 360f
        if (delta > 180f) delta -= 360f
        if (delta < -180f) delta += 360f
        animatedDegrees.animateTo(current + delta, tween(300))
    }
    val headingAngle = animatedDegrees.value

    Canvas(modifier = modifier) {
        val d = minOf(size.width, size.height)
        val cx = size.width / 2f; val cy = size.height / 2f
        val outerR = d / 2f - 30f * density
        fun dp(v: Float) = v * density

        drawCircle(color = DialFace, radius = outerR, center = Offset(cx, cy))

        rotate(-headingAngle, pivot = Offset(cx, cy)) {
            for (i in 0 until 360 step 2) {
                if (i % 10 == 0) continue
                val a = (i - 90) * PI / 180.0; val c = cos(a).toFloat(); val s = sin(a).toFloat()
                drawLine(color = DialGrey, start = Offset(cx + (outerR - dp(10f)) * c, cy + (outerR - dp(10f)) * s), end = Offset(cx + outerR * c, cy + outerR * s), strokeWidth = dp(1.5f))
            }
            for (i in 0 until 360 step 10) {
                if (i % 30 == 0) continue
                val a = (i - 90) * PI / 180.0; val c = cos(a).toFloat(); val s = sin(a).toFloat()
                drawLine(color = White.copy(alpha = 0.85f), start = Offset(cx + (outerR - dp(13f)) * c, cy + (outerR - dp(13f)) * s), end = Offset(cx + (outerR + dp(2f)) * c, cy + (outerR + dp(2f)) * s), strokeWidth = dp(2f))
            }
            for (i in 0 until 360 step 30) {
                val a = (i - 90) * PI / 180.0; val c = cos(a).toFloat(); val s = sin(a).toFloat()
                drawLine(color = White, start = Offset(cx + (outerR - dp(15f)) * c, cy + (outerR - dp(15f)) * s), end = Offset(cx + (outerR + dp(6f)) * c, cy + (outerR + dp(6f)) * s), strokeWidth = dp(3f))
            }
            val nc = cos((-90.0) * PI / 180.0).toFloat(); val ns = sin((-90.0) * PI / 180.0).toFloat()
            drawLine(color = Red, start = Offset(cx + (outerR - dp(4f)) * nc, cy + (outerR - dp(4f)) * ns), end = Offset(cx + (outerR + dp(24f)) * nc, cy + (outerR + dp(24f)) * ns), strokeWidth = dp(4.5f))
        }

        for (i in 0 until 360 step 30) {
            val screenAngle = i - headingAngle
            val a = (screenAngle - 90) * PI / 180.0; val c = cos(a).toFloat(); val s = sin(a).toFloat()
            if (i % 90 == 0) {
                val label = when (i) { 0 -> "N"; 90 -> "E"; 180 -> "S"; else -> "W" }
                val r = outerR - dp(48f)
                val style = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Normal, color = White)
                val measured = textMeasurer.measure(label, style)
                drawText(measured, topLeft = Offset(cx + r * c - measured.size.width / 2f, cy + r * s - measured.size.height / 2f))
            }
            if (i != 0) {
                val r = outerR + dp(20f)
                val style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal, color = White.copy(alpha = 0.95f))
                val measured = textMeasurer.measure(i.toString(), style)
                drawText(measured, topLeft = Offset(cx + r * c - measured.size.width / 2f, cy + r * s - measured.size.height / 2f))
            }
        }

        val crossHalf = outerR * 0.23f
        drawCircle(color = DialGrey, radius = dp(7f), center = Offset(cx, cy))
        drawLine(color = White, start = Offset(cx - crossHalf, cy), end = Offset(cx + crossHalf, cy), strokeWidth = dp(2f))
        drawLine(color = White, start = Offset(cx, cy - crossHalf), end = Offset(cx, cy + crossHalf), strokeWidth = dp(2f))

        val levelRadius = dp(20f)
        drawCircle(color = Color(0xFF222222), radius = levelRadius, center = Offset(cx, cy), style = Stroke(width = dp(1.5f)))
        val maxTilt = 15f; val maxOffset = levelRadius - dp(3f)
        val rollOffset = (roll / maxTilt).coerceIn(-1f, 1f) * maxOffset
        val pitchOffset = (pitch / maxTilt).coerceIn(-1f, 1f) * maxOffset
        val bubbleX = cx + rollOffset; val bubbleY = cy - pitchOffset; val bubbleRadius = dp(8f)
        drawCircle(color = Color(0xFF00E676), radius = bubbleRadius, center = Offset(bubbleX, bubbleY))
        drawCircle(color = Color.White.copy(alpha = 0.6f), radius = bubbleRadius * 0.3f, center = Offset(bubbleX - dp(2f), bubbleY - dp(2f)))
    }
}