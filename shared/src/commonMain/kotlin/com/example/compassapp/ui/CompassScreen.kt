package com.example.compassapp.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
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

// ── Palette ─────────────────────────────────────────────────────────────────
private val BgTop = Color(0xFF0B0F1A)
private val BgBottom = Color(0xFF121826)
private val Cyan = Color(0xFF32E0C4)
private val CyanDim = Color(0xFF1E8F82)
private val Coral = Color(0xFFFF5C6C)
private val Amber = Color(0xFFFFB020)
private val Ink = Color(0xFFEAF0F7)
private val InkFaint = Color(0xFF7C8AA0)
private val GlassFill = Color(0xFFFFFFFF)
private val CardStroke = Color(0xFFFFFFFF)

@Composable
fun CompassApp(repository: CompassRepository = remember { CompassRepository() }) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Cyan,
            secondary = Amber,
            error = Coral,
            background = BgTop,
            surface = BgBottom,
            onBackground = Ink,
            onSurface = Ink
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(BgTop, BgBottom)))
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
    var isRunning by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose { repository.stopAll() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(top = 28.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "COMPASS",
            style = TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 4.sp,
                color = InkFaint
            )
        )

        Spacer(modifier = Modifier.height(18.dp))

        HeadingReadout(heading = heading, error = headingError)

        Spacer(modifier = Modifier.height(8.dp))

        Box(modifier = Modifier.fillMaxWidth(0.86f).aspectRatio(1f)) {
            CircularCompass(heading = heading, modifier = Modifier.fillMaxSize())
        }

        Spacer(modifier = Modifier.height(20.dp))

        LocationCard(
            location = location,
            locationError = locationError,
            placeName = placeName,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        TrackingButton(
            isRunning = isRunning,
            onClick = {
                if (isRunning) repository.stopAll() else repository.startAll()
                isRunning = !isRunning
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Heading readout: big animated degree number + cardinal pill
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun HeadingReadout(heading: HeadingData?, error: String?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = heading?.let { "${it.degrees.toDouble().roundTo(0).toInt()}" } ?: "—",
            style = TextStyle(
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold,
                color = Ink,
                fontFamily = FontFamily.Monospace
            )
        )
        Text(
            text = "°",
            style = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Cyan),
            modifier = Modifier.padding(start = 2.dp, top = 6.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Brush.horizontalGradient(listOf(CyanDim.copy(alpha = 0.35f), Cyan.copy(alpha = 0.18f))))
                .border(1.dp, Cyan.copy(alpha = 0.4f), RoundedCornerShape(50))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = heading?.cardinalDirection ?: "—",
                style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Cyan)
            )
        }
    }
    if (error != null) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = error, color = Coral, style = TextStyle(fontSize = 12.sp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Circular compass: glowing dial + smoothly animated needle
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CircularCompass(
    heading: HeadingData?,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val target = heading?.degrees ?: 0f

    // Animate the shortest angular path so the needle never spins the long way around.
    val animatedDegrees = remember { Animatable(target) }
    LaunchedEffect(target) {
        val current = animatedDegrees.value
        var delta = (target - current) % 360f
        if (delta > 180f) delta -= 360f
        if (delta < -180f) delta += 360f
        animatedDegrees.animateTo(current + delta, animationSpec = tween(durationMillis = 220))
    }
    val degrees = animatedDegrees.value

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasSize = minOf(size.width, size.height)
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val outerRadius = canvasSize / 2f - 6f
            val innerRadius = outerRadius - 26f

            // ── Soft ambient glow behind the dial ──────────────────────
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Cyan.copy(alpha = 0.16f), Color.Transparent),
                    center = Offset(centerX, centerY),
                    radius = outerRadius * 1.35f
                ),
                radius = outerRadius * 1.35f,
                center = Offset(centerX, centerY)
            )

            // ── Outer bezel: dark glass ring with subtle gradient ──────
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF1A2233), Color(0xFF0E1420)),
                    center = Offset(centerX, centerY - outerRadius * 0.3f),
                    radius = outerRadius * 1.6f
                ),
                radius = outerRadius,
                center = Offset(centerX, centerY)
            )
            drawCircle(
                color = Cyan.copy(alpha = 0.25f),
                radius = outerRadius,
                center = Offset(centerX, centerY),
                style = Stroke(width = 1.5f)
            )

            // ── Inner face ──────────────────────────────────────────────
            drawCircle(
                color = Color(0xFF0C111C),
                radius = innerRadius,
                center = Offset(centerX, centerY)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.06f),
                radius = innerRadius,
                center = Offset(centerX, centerY),
                style = Stroke(width = 1f)
            )

            // ── Tick marks (fixed, 0° at top) ──────────────────────────
            for (i in 0 until 360 step 5) {
                val isCardinal = i % 90 == 0
                val isMajor = i % 30 == 0
                val isMinor = i % 10 == 0

                val tickLength = when {
                    isCardinal -> 22f
                    isMajor -> 15f
                    isMinor -> 9f
                    else -> 4f
                }
                val tickWidth = when {
                    isCardinal -> 3f
                    isMajor -> 1.8f
                    else -> 1f
                }
                val tickColor = when {
                    isCardinal -> Cyan
                    isMajor -> Ink.copy(alpha = 0.55f)
                    else -> Ink.copy(alpha = 0.2f)
                }

                val angleRad = (i - 90) * PI / 180.0
                val outerX = centerX + outerRadius * cos(angleRad).toFloat()
                val outerY = centerY + outerRadius * sin(angleRad).toFloat()
                val innerX = centerX + (outerRadius - tickLength) * cos(angleRad).toFloat()
                val innerY = centerY + (outerRadius - tickLength) * sin(angleRad).toFloat()

                drawLine(
                    color = tickColor,
                    start = Offset(innerX, innerY),
                    end = Offset(outerX, outerY),
                    strokeWidth = tickWidth
                )
            }

            // ── Cardinal / degree labels ────────────────────────────────
            val numberRadius = innerRadius - 20f
            for (i in 0 until 360 step 30) {
                val angleRad = (i - 90) * PI / 180.0
                val x = centerX + numberRadius * cos(angleRad).toFloat()
                val y = centerY + numberRadius * sin(angleRad).toFloat()

                val label = when (i) {
                    0 -> "N"
                    90 -> "E"
                    180 -> "S"
                    270 -> "W"
                    else -> i.toString()
                }
                val isCardinal = i % 90 == 0
                val style = TextStyle(
                    fontSize = (canvasSize / (if (isCardinal) 17f else 30f)).sp,
                    fontWeight = if (isCardinal) FontWeight.Bold else FontWeight.Normal,
                    color = if (i == 0) Coral else Ink.copy(alpha = if (isCardinal) 0.95f else 0.45f)
                )
                val measured = textMeasurer.measure(label, style = style)
                drawText(
                    textLayoutResult = measured,
                    topLeft = Offset(x - measured.size.width / 2f, y - measured.size.height / 2f)
                )
            }

            // ── Needle (rotates with heading) ──────────────────────────
            rotate(degrees = degrees, pivot = Offset(centerX, centerY)) {
                val needleLength = innerRadius - 34f
                val halfWidth = 9f

                // Glow layer behind the needle
                val glowPath = Path().apply {
                    moveTo(centerX, centerY - needleLength - 4f)
                    lineTo(centerX - halfWidth - 4f, centerY)
                    lineTo(centerX, centerY + needleLength + 4f)
                    lineTo(centerX + halfWidth + 4f, centerY)
                    close()
                }
                drawPath(path = glowPath, color = Coral.copy(alpha = 0.18f))

                val northPath = Path().apply {
                    moveTo(centerX, centerY - needleLength)
                    lineTo(centerX - halfWidth, centerY)
                    lineTo(centerX + halfWidth, centerY)
                    close()
                }
                drawPath(
                    path = northPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(Coral, Amber),
                        startY = centerY - needleLength,
                        endY = centerY
                    )
                )

                val southPath = Path().apply {
                    moveTo(centerX, centerY + needleLength)
                    lineTo(centerX - halfWidth, centerY)
                    lineTo(centerX + halfWidth, centerY)
                    close()
                }
                drawPath(path = southPath, color = Ink.copy(alpha = 0.28f))
            }

            // ── Center hub ──────────────────────────────────────────────
            drawCircle(color = Color(0xFF0C111C), radius = 13f, center = Offset(centerX, centerY))
            drawCircle(
                color = Cyan.copy(alpha = 0.35f),
                radius = 13f,
                center = Offset(centerX, centerY),
                style = Stroke(width = 1.5f)
            )
            drawCircle(color = Cyan, radius = 5f, center = Offset(centerX, centerY))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Location card: glassmorphic surface with coordinates
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun LocationCard(
    location: dev.jordond.compass.Location?,
    locationError: String?,
    placeName: String?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(GlassFill.copy(alpha = 0.05f))
            .border(1.dp, CardStroke.copy(alpha = 0.10f), RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Brush.radialGradient(listOf(Cyan.copy(alpha = 0.35f), Cyan.copy(alpha = 0.05f)))),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(Cyan)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "LOCATION",
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 2.sp,
                        color = InkFaint
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            when {
                location != null -> {
                    val coords = location.coordinates

                    if (placeName != null) {
                        Text(
                            text = placeName,
                            style = TextStyle(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Ink
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        CoordinateBlock(label = "LATITUDE", value = coords.latitude.roundTo(6).toString())
                        CoordinateBlock(label = "LONGITUDE", value = coords.longitude.roundTo(6).toString())
                    }
                }
                locationError != null -> {
                    Text(text = "Error: $locationError", color = Coral, style = TextStyle(fontSize = 14.sp))
                }
                else -> {
                    Text(
                        text = "Tap Start to begin tracking",
                        style = TextStyle(fontSize = 14.sp, color = InkFaint)
                    )
                }
            }
        }
    }
}

@Composable
private fun CoordinateBlock(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = TextStyle(
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp,
                color = InkFaint
            )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = TextStyle(
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                color = Ink
            )
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Start/Stop button: gradient pill
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun TrackingButton(isRunning: Boolean, onClick: () -> Unit) {
    val gradient = if (isRunning) {
        Brush.horizontalGradient(listOf(Coral, Amber))
    } else {
        Brush.horizontalGradient(listOf(Cyan, CyanDim))
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(gradient)
            .clickable(onClick = onClick)
            .padding(horizontal = 40.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (isRunning) "STOP TRACKING" else "START TRACKING",
            style = TextStyle(
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = Color(0xFF0B0F1A)
            )
        )
    }
}
