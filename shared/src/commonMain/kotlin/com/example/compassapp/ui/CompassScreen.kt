package com.example.compassapp.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.compassapp.compass.CompassRepository
import com.example.compassapp.compass.HeadingData // <-- Ensures HeadingData is resolved
import com.example.compassapp.compass.roundTo
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CompassApp(repository: CompassRepository = remember { CompassRepository() }) {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
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
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Heading readout
        Text(
            text = heading?.let { "${it.formatDegrees()}  ${it.cardinalDirection}" } ?: "—",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
        )
        if (headingError != null) {
            Text(
                text = headingError!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Circular compass dial with arrow ──────────────────────────
        CircularCompass(
            heading = heading,
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .aspectRatio(1f)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ── Location card ─────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("📍 Location", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                if (location != null) {
                    val coords = location!!.coordinates

                    // Place name (from reverse geocoding)
                    if (placeName != null) {
                        Text(
                            text = placeName!!,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    // Latitude / Longitude side-by-side
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Latitude", style = MaterialTheme.typography.labelSmall)
                            Text(
                                coords.latitude.roundTo(6).toString(),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        Column {
                            Text("Longitude", style = MaterialTheme.typography.labelSmall)
                            Text(
                                coords.longitude.roundTo(6).toString(),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                } else if (locationError != null) {
                    Text("Error: $locationError", color = MaterialTheme.colorScheme.error)
                } else {
                    Text("—")
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(onClick = {
            if (isRunning) {
                repository.stopAll()
            } else {
                repository.startAll()
            }
            isRunning = !isRunning
        }) {
            Text(if (isRunning) "Stop" else "Start")
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Circular compass: fixed 0-360° dial + rotating arrow needle
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CircularCompass(
    heading: HeadingData?,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val degrees = heading?.degrees ?: 0f

    val surface = MaterialTheme.colorScheme.surface
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface
    val primary = MaterialTheme.colorScheme.primary
    val error = MaterialTheme.colorScheme.error

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasSize = minOf(size.width, size.height)
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val outerRadius = canvasSize / 2f - 8f
            val innerRadius = outerRadius - 20f

            // ── Outer bezel ───────────────────────────────────────────
            drawCircle(
                color = surfaceVariant,
                radius = outerRadius,
                center = Offset(centerX, centerY)
            )
            drawCircle(
                color = onSurface.copy(alpha = 0.4f),
                radius = outerRadius,
                center = Offset(centerX, centerY),
                style = Stroke(width = 2f)
            )

            // ── Inner face ────────────────────────────────────────────
            drawCircle(
                color = surface,
                radius = innerRadius,
                center = Offset(centerX, centerY)
            )
            drawCircle(
                color = onSurface.copy(alpha = 0.15f),
                radius = innerRadius,
                center = Offset(centerX, centerY),
                style = Stroke(width = 1f)
            )

            // ── Tick marks (fixed, 0° at top) ─────────────────────────
            for (i in 0 until 360 step 5) {
                val isCardinal = i % 90 == 0
                val isMajor = i % 30 == 0
                val isMinor = i % 10 == 0

                val tickLength = when {
                    isCardinal -> 24f
                    isMajor    -> 16f
                    isMinor    -> 9f
                    else       -> 5f
                }
                val tickWidth = when {
                    isCardinal -> 3.5f
                    isMajor    -> 2f
                    else       -> 1f
                }
                val tickColor = when {
                    isCardinal -> onSurface
                    isMajor    -> onSurface.copy(alpha = 0.7f)
                    else       -> onSurface.copy(alpha = 0.35f)
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

            // ── Degree numbers every 30° ──────────────────────────────
            val numberRadius = innerRadius - 14f
            for (i in 0 until 360 step 30) {
                val angleRad = (i - 90) * PI / 180.0
                val x = centerX + numberRadius * cos(angleRad).toFloat()
                val y = centerY + numberRadius * sin(angleRad).toFloat()

                val label = when (i) {
                    0   -> "N"
                    90  -> "E"
                    180 -> "S"
                    270 -> "W"
                    else -> i.toString()
                }

                val isCardinal = i % 90 == 0
                val style = TextStyle(
                    fontSize = (canvasSize / (if (isCardinal) 20f else 30f)).sp,
                    fontWeight = if (isCardinal) FontWeight.Bold else FontWeight.Normal,
                    color = if (i == 0) error else onSurface.copy(alpha = if (isCardinal) 1f else 0.7f)
                )

                val measured = textMeasurer.measure(label, style = style)
                translate(
                    left = x - measured.size.width / 2f,
                    top = y - measured.size.height / 2f
                ) {
                    drawText(measured)
                }
            }

            // ── Arrow needle (rotates with heading) ───────────────────
            rotate(degrees = degrees, pivot = Offset(centerX, centerY)) {
                val needleLength = innerRadius - 30f
                val needleHalfWidth = 10f

                val northPath = Path().apply {
                    moveTo(centerX, centerY - needleLength)
                    lineTo(centerX - needleHalfWidth, centerY)
                    lineTo(centerX + needleHalfWidth, centerY)
                    close()
                }
                drawPath(path = northPath, color = error)

                val southPath = Path().apply {
                    moveTo(centerX, centerY + needleLength)
                    lineTo(centerX - needleHalfWidth, centerY)
                    lineTo(centerX + needleHalfWidth, centerY)
                    close()
                }
                drawPath(path = southPath, color = onSurface.copy(alpha = 0.35f))

                val fullNeedle = Path().apply {
                    moveTo(centerX, centerY - needleLength)
                    lineTo(centerX - needleHalfWidth, centerY)
                    lineTo(centerX, centerY + needleLength)
                    lineTo(centerX + needleHalfWidth, centerY)
                    close()
                }
                drawPath(
                    path = fullNeedle,
                    color = onSurface.copy(alpha = 0.25f),
                    style = Stroke(width = 1.5f)
                )
            }

            // ── Center hub ────────────────────────────────────────────
            drawCircle(
                color = onSurface,
                radius = 10f,
                center = Offset(centerX, centerY)
            )
            drawCircle(
                color = primary,
                radius = 5f,
                center = Offset(centerX, centerY)
            )
        }
    }
}