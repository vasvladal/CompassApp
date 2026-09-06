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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
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

// Normalize angle to 0-360 range
private fun normalizeAngle(angle: Float): Float {
    var result = angle % 360f
    if (result < 0) result += 360f
    return result
}

// Convert decimal degrees to DMS (Degrees, Minutes, Seconds)
private fun decimalToDMS(value: Double): Triple<Int, Int, Double> {
    val degrees = value.toInt()
    val minutesDecimal = abs((value - degrees) * 60)
    val minutes = minutesDecimal.toInt()
    val seconds = (minutesDecimal - minutes) * 60
    return Triple(abs(degrees), minutes, seconds)
}

// Format DMS as string
private fun formatDMS(degrees: Int, minutes: Int, seconds: Double): String {
    return "${degrees}°${minutes.toString().padStart(2, '0')}'${seconds.roundTo(2).toString().padStart(5, '0')}\""
}

@Composable
fun CompassApp(repository: CompassRepository = remember { CompassRepository() }) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Purple,
            background = Black,
            surface = Black,
            onBackground = White,
            onSurface = White
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Black)
        ) {
            CompassScreen(repository)
        }
    }
}

@Composable
private fun CompassScreen(repository: CompassRepository) {
    val location by repository.location.collectAsState()
    val locationError by repository.locationError.collectAsState()
    val placeName by repository.placeName.collectAsState()
    val heading by repository.heading.collectAsState()
    val headingError by repository.headingError.collectAsState()

    // UI state
    var showLocationDetails by remember { mutableStateOf(false) }
    var calibrationPoints by remember { mutableStateOf(listOf<Pair<Float, Float>>()) }

    // Get raw heading from sensor
    val rawHeading = heading?.degrees ?: 0f

    // Apply calibration using interpolation
    val correctedHeading = calibrateHeading(rawHeading, calibrationPoints)

    DisposableEffect(Unit) {
        repository.startAll()
        onDispose { repository.stopAll() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(0.08f))

            // Location name with click to show details
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .clickable { showLocationDetails = !showLocationDetails }
                    .padding(4.dp)
            ) {
                Text(
                    text = placeName ?: "Locating…",
                    color = White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "📍",
                    fontSize = 14.sp
                )
            }

            // Location details (expanded)
            if (showLocationDetails && location != null) {
                val lat = location!!.coordinates.latitude
                val lon = location!!.coordinates.longitude
                val (latDeg, latMin, latSec) = decimalToDMS(lat)
                val (lonDeg, lonMin, lonSec) = decimalToDMS(lon)
                val latDir = if (lat >= 0) "N" else "S"
                val lonDir = if (lon >= 0) "E" else "W"

                // Get additional info - these are likely Float or Double
                val altitude = location!!.mslAltitude
                val accuracy = location!!.accuracy
                val speed = location!!.speed

                Column(
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .background(DarkGray.copy(alpha = 0.3f))
                        .padding(12.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "📍 Location Details",
                        color = Purple,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))

                    // Latitude
                    Text(
                        text = "Latitude: ${formatDMS(latDeg, latMin, latSec)} $latDir",
                        color = White,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "         ${abs(lat)}° (decimal)",
                        color = LightGray,
                        fontSize = 11.sp
                    )

                    Spacer(Modifier.height(2.dp))

                    // Longitude
                    Text(
                        text = "Longitude: ${formatDMS(lonDeg, lonMin, lonSec)} $lonDir",
                        color = White,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "          ${abs(lon)}° (decimal)",
                        color = LightGray,
                        fontSize = 11.sp
                    )

                    Spacer(Modifier.height(4.dp))

                    // Additional info - use the values directly as Double
                    if (altitude != null) {
                        val altValue = altitude as Double
                        Text(
                            text = "Altitude: ${altValue.roundTo(1)} m",
                            color = LightGray,
                            fontSize = 12.sp
                        )
                    }
                    if (accuracy != null) {
                        val accValue = accuracy as Double
                        Text(
                            text = "Accuracy: ±${accValue.roundTo(1)} m",
                            color = LightGray,
                            fontSize = 12.sp
                        )
                    }
                    if (speed != null) {
                        val spdValue = speed as Double
                        val speedKmh = spdValue * 3.6
                        Text(
                            text = "Speed: ${speedKmh.roundTo(1)} km/h",
                            color = LightGray,
                            fontSize = 12.sp
                        )
                    }

                    // Refresh button
                    Button(
                        onClick = { repository.refreshLocation() },
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text("Refresh Location", fontSize = 12.sp)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // "TRUE HEADING" label
            Box(
                modifier = Modifier
                    .height(32.dp)
                    .width(168.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Purple),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "TRUE HEADING",
                    color = PurpleText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(12.dp))

            // Heading readout
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Text(
                    text = correctedHeading.toDouble().roundTo(0).toInt().toString(),
                    color = White,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Normal
                )
                Text(
                    "°",
                    modifier = Modifier.padding(bottom = 4.dp, start = 2.dp),
                    color = White,
                    fontSize = 24.sp
                )
                Spacer(Modifier.width(8.dp))
                val correctedHeadingData = HeadingData.fromDegrees(correctedHeading)
                Text(
                    correctedHeadingData.cardinalDirection,
                    modifier = Modifier.padding(bottom = 4.dp),
                    color = White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Normal
                )
            }

            // Debug info and calibration
            Row(
                modifier = Modifier.padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Raw: ${rawHeading.toInt()}°",
                    color = Color.Gray,
                    fontSize = 10.sp
                )
                Text(
                    text = "Points: ${calibrationPoints.size}",
                    color = Color.Gray,
                    fontSize = 10.sp
                )
            }

            // Calibration controls
            Row(
                modifier = Modifier.padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        val trueHeading = correctedHeading
                        calibrationPoints = calibrationPoints + (rawHeading to trueHeading)
                    },
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("Add Point", fontSize = 10.sp)
                }
                Button(
                    onClick = { calibrationPoints = emptyList() },
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("Clear", fontSize = 10.sp)
                }
            }

            // Show calibration points
            if (calibrationPoints.isNotEmpty()) {
                Text(
                    text = calibrationPoints.take(3).joinToString {
                        "(${it.first.toInt()}°→${it.second.toInt()}°)"
                    } + if (calibrationPoints.size > 3) " …" else "",
                    color = Color.Gray,
                    fontSize = 9.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            if (headingError != null) {
                Text(headingError ?: "", color = Color(0xFFFF5A60), fontSize = 10.sp)
            }
            if (locationError != null) {
                Text(locationError ?: "", color = Color(0xFFFF5A60), fontSize = 10.sp)
            }

            Spacer(Modifier.height(6.dp))

            // Separator line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFF383838))
            )

            // Fixed heading marker
            Box(
                modifier = Modifier
                    .height(18.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                Canvas(Modifier.size(14.dp)) {
                    val p = Path().apply {
                        moveTo(size.width / 2f, size.height)
                        lineTo(0f, 0f)
                        lineTo(size.width, 0f)
                        close()
                    }
                    drawPath(p, color = White)
                }
            }

            Spacer(Modifier.height(6.dp))

            // Compass dial
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.5f),
                contentAlignment = Alignment.Center
            ) {
                CircularCompass(
                    heading = heading,
                    calibrationPoints = calibrationPoints,
                    modifier = Modifier.size(280.dp)
                )
            }

            Spacer(Modifier.height(6.dp))

            // Strength indicator
            Text(
                text = "STRENGTH ${heading?.magneticStrengthMicroTesla?.let { "${it.toDouble().roundTo(0).toInt()} μT" } ?: "—"}",
                color = White,
                fontSize = 13.sp,
                letterSpacing = 0.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            Spacer(Modifier.weight(0.08f))
        }
    }
}

// Calibration function using linear interpolation between points
private fun calibrateHeading(raw: Float, points: List<Pair<Float, Float>>): Float {
    if (points.isEmpty()) return normalizeAngle(raw)
    if (points.size == 1) {
        val (sensor, trueHeading) = points.first()
        return normalizeAngle(raw + (trueHeading - sensor))
    }

    val sorted = points.sortedBy { it.first }

    var i = 0
    while (i < sorted.size - 1 && sorted[i + 1].first < raw) {
        i++
    }

    if (i == 0 && raw < sorted[0].first) {
        return normalizeAngle(raw + (sorted[0].second - sorted[0].first))
    }
    if (i >= sorted.size - 1) {
        val last = sorted.last()
        return normalizeAngle(raw + (last.second - last.first))
    }

    val (sensor1, true1) = sorted[i]
    val (sensor2, true2) = sorted[i + 1]

    val t = (raw - sensor1) / (sensor2 - sensor1)
    val corrected = lerp(true1, true2, t)

    return normalizeAngle(corrected)
}

private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

@Composable
private fun CircularCompass(
    heading: HeadingData?,
    calibrationPoints: List<Pair<Float, Float>> = emptyList(),
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val rawTarget = heading?.degrees ?: 0f
    val target = calibrateHeading(rawTarget, calibrationPoints)

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
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = d / 2f - 8f

        drawCircle(
            color = DialFace,
            radius = radius,
            center = Offset(cx, cy)
        )

        rotate(-headingAngle, pivot = Offset(cx, cy)) {
            // Ticks
            for (i in 0 until 360 step 5) {
                val major = i % 30 == 0
                val cardinal = i % 90 == 0
                val len = when {
                    cardinal -> 25f
                    major -> 17f
                    else -> 10f
                }
                val width = when {
                    cardinal -> 4f
                    major -> 2.4f
                    else -> 1.7f
                }
                val a = (i - 90) * PI / 180.0
                val ox = cx + radius * cos(a).toFloat()
                val oy = cy + radius * sin(a).toFloat()
                val ix = cx + (radius - len) * cos(a).toFloat()
                val iy = cy + (radius - len) * sin(a).toFloat()

                drawLine(
                    color = if (major || cardinal) DialGrey else DialGrey.copy(alpha = .62f),
                    start = Offset(ix, iy),
                    end = Offset(ox, oy),
                    strokeWidth = width
                )
            }

            // Labels
            val labelRadius = radius - 45f
            for (i in 0 until 360 step 30) {
                val a = (i - 90) * PI / 180.0
                val x = cx + labelRadius * cos(a).toFloat()
                val y = cy + labelRadius * sin(a).toFloat()

                val label = when (i) {
                    0 -> "N"
                    30 -> "30"
                    60 -> "60"
                    90 -> "E"
                    120 -> "120"
                    150 -> "150"
                    180 -> "S"
                    210 -> "210"
                    240 -> "240"
                    270 -> "W"
                    300 -> "300"
                    330 -> "330"
                    else -> i.toString()
                }
                val cardinal = i % 90 == 0
                val style = TextStyle(
                    fontSize = if (cardinal) 24.sp else 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = if (cardinal) White else White.copy(alpha = .88f)
                )
                val measured = textMeasurer.measure(label, style)
                drawText(
                    measured,
                    topLeft = Offset(
                        x - measured.size.width / 2f,
                        y - measured.size.height / 2f
                    )
                )
            }

            // Red north indicator
            val northAngle = (-90.0) * PI / 180.0
            val nx = cx + (radius - 8f) * cos(northAngle).toFloat()
            val ny = cy + (radius - 8f) * sin(northAngle).toFloat()
            val nix = cx + (radius - 40f) * cos(northAngle).toFloat()
            val niy = cy + (radius - 40f) * sin(northAngle).toFloat()
            drawLine(
                color = Red,
                start = Offset(nix, niy),
                end = Offset(nx, ny),
                strokeWidth = 4.5f
            )

            // White tail
            val tailX = cx - (radius - 40f) * cos(northAngle).toFloat()
            val tailY = cy - (radius - 40f) * sin(northAngle).toFloat()
            drawLine(
                color = White.copy(alpha = 0.4f),
                start = Offset(cx, cy),
                end = Offset(tailX, tailY),
                strokeWidth = 3f
            )
        }

        drawCircle(
            color = White,
            radius = 6f,
            center = Offset(cx, cy)
        )

        // Top indicator triangle
        val trianglePath = Path().apply {
            moveTo(cx, cy - radius + 10f)
            lineTo(cx - 12f, cy - radius + 30f)
            lineTo(cx + 12f, cy - radius + 30f)
            close()
        }
        drawPath(trianglePath, color = White)
    }
}