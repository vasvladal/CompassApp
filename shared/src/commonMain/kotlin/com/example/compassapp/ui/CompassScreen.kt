package com.example.compassapp.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.Stroke
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

private val Black = Color(0xFF000000)
private val White = Color(0xFFF4F1FA)
private val Muted = Color(0xFFB8B4C0)
private val Purple = Color(0xFFD0B7FF)
private val PurpleText = Color(0xFF34224E)
private val Yellow = Color(0xFFFFE500)
private val Red = Color(0xFFFF0010)
private val DialGrey = Color(0xFF888888)
private val DialFace = Color(0xFF030303)

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
    var isRunning by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        repository.startAll()
        isRunning = true
        onDispose { repository.stopAll() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp)
            .padding(top = 18.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top mode pill
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Purple)
                .padding(horizontal = 44.dp, vertical = 15.dp)
        ) {
            Text(
                text = "TRUE HEADING",
                color = PurpleText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(Modifier.height(174.dp))

        // Place + information button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = placeName ?: "Locating…",
                color = White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal
            )
            Spacer(Modifier.width(8.dp))
            InfoIcon()
        }

        Spacer(Modifier.height(28.dp))

        HeadingReadout(heading, headingError)

        Spacer(Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true),
            contentAlignment = Alignment.TopCenter
        ) {
            CircularCompass(
                heading = heading,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 0.dp)
                    .aspectRatio(1f)
            )

            // Fixed heading marker, exactly like the reference UI.
            Canvas(
                Modifier
                    .size(24.dp)
                    .offset(y = 0.dp)
                    .align(Alignment.TopCenter)
            ) {
                val p = Path().apply {
                    moveTo(size.width / 2f, size.height)
                    lineTo(0f, 0f)
                    lineTo(size.width, 0f)
                    close()
                }
                drawPath(p, color = White)
            }
        }

        Spacer(Modifier.height(12.dp))

        // Bottom status row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "STRENGTH",
                    color = Muted,
                    fontSize = 15.sp,
                    letterSpacing = 1.sp
                )
                Text(
                    text = heading?.magneticStrengthMicroTesla?.let {
                        "${it.toDouble().roundTo(0).toInt()} μT"
                    } ?: "—",
                    color = White,
                    fontSize = 17.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            CircleButton(onClick = { /* Map action hook */ }) {
                MapIcon()
            }
            Spacer(Modifier.width(10.dp))
            CircleButton(onClick = {
                if (isRunning) repository.stopAll() else repository.startAll()
                isRunning = !isRunning
            }) {
                GearIcon()
            }
        }

        if (locationError != null || headingError != null) {
            Spacer(Modifier.height(5.dp))
            Text(
                locationError ?: headingError ?: "",
                color = Color(0xFFFF5A60),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun HeadingReadout(heading: HeadingData?, error: String?) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = heading?.let { it.degrees.toDouble().roundTo(0).toInt().toString() } ?: "—",
            color = White,
            fontSize = 52.sp,
            fontWeight = FontWeight.Normal,
            fontFamily = FontFamily.Default
        )
        Text(
            "°",
            modifier = Modifier.padding(bottom = 5.dp, start = 2.dp),
            color = White,
            fontSize = 27.sp
        )
        Spacer(Modifier.width(9.dp))
        Text(
            heading?.cardinalDirection ?: "—",
            modifier = Modifier.padding(bottom = 5.dp),
            color = White,
            fontSize = 27.sp,
            fontWeight = FontWeight.Normal
        )
    }
    if (error != null) {
        Text(error, color = Color(0xFFFF5A60), fontSize = 11.sp)
    }
}

@Composable
private fun CircularCompass(
    heading: HeadingData?,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val target = heading?.degrees ?: 0f
    val animatedDegrees = remember { Animatable(target) }

    LaunchedEffect(target) {
        val current = animatedDegrees.value
        var delta = (target - current) % 360f
        if (delta > 180f) delta -= 360f
        if (delta < -180f) delta += 360f
        animatedDegrees.animateTo(current + delta, tween(180))
    }

    val degrees = animatedDegrees.value

    Canvas(modifier = modifier) {
        val d = minOf(size.width, size.height)
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = d / 2f - 10f

        drawCircle(
            color = DialFace,
            radius = radius,
            center = Offset(cx, cy)
        )

        // The dial itself follows the device, while the top marker stays fixed.
        rotate(-degrees, pivot = Offset(cx, cy)) {
            // Fine and major tick ring
            for (i in 0 until 360 step 5) {
                val major = i % 30 == 0
                val medium = i % 10 == 0
                val cardinal = i % 90 == 0
                val len = when {
                    cardinal -> 25f
                    major -> 17f
                    medium -> 10f
                    else -> 5f
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

            // Degree/cardinal labels
            val labelRadius = radius - 49f
            for (i in 0 until 360 step 30) {
                val a = (i - 90) * PI / 180.0
                val x = cx + labelRadius * cos(a).toFloat()
                val y = cy + labelRadius * sin(a).toFloat()
                val label = when (i) {
                    0 -> "N"
                    90 -> "E"
                    180 -> "S"
                    270 -> "W"
                    else -> i.toString()
                }
                val cardinal = i % 90 == 0
                val style = TextStyle(
                    fontSize = if (cardinal) 31.sp else 15.sp,
                    fontWeight = if (cardinal) FontWeight.Normal else FontWeight.Normal,
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

            // Red north indicator, thin and deliberately offset from the labels.
            val northAngle = (-90.0) * PI / 180.0
            val nx = cx + (radius - 8f) * cos(northAngle).toFloat()
            val ny = cy + (radius - 8f) * sin(northAngle).toFloat()
            val nix = cx + (radius - 50f) * cos(northAngle).toFloat()
            val niy = cy + (radius - 50f) * sin(northAngle).toFloat()
            drawLine(
                color = Red,
                start = Offset(nix, niy),
                end = Offset(nx, ny),
                strokeWidth = 5f
            )
        }
    }
}

@Composable
private fun InfoIcon() {
    Box(
        modifier = Modifier
            .size(29.dp)
            .border(3.dp, Yellow, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "i",
            color = Yellow,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun CircleButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(54.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF262626))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun MapIcon() {
    Canvas(Modifier.size(27.dp)) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(2f, 4f)
            lineTo(9f, 2f)
            lineTo(18f, 5f)
            lineTo(25f, 2f)
            lineTo(25f, 21f)
            lineTo(18f, 24f)
            lineTo(9f, 21f)
            lineTo(2f, 24f)
            close()
        }
        drawPath(path, color = Purple, style = Stroke(width = 2.2f))
        drawLine(Purple, Offset(9f, 2f), Offset(9f, 21f), 2f)
        drawLine(Purple, Offset(18f, 5f), Offset(18f, 24f), 2f)
    }
}

@Composable
private fun GearIcon() {
    Canvas(Modifier.size(28.dp)) {
        val c = Offset(size.width / 2f, size.height / 2f)
        drawCircle(Purple, 8f, c)
        drawCircle(Color(0xFF262626), 3f, c)
        for (i in 0 until 8) {
            rotate(i * 45f, c) {
                drawRect(
                    color = Purple,
                    topLeft = Offset(c.x - 2.5f, 1f),
                    size = androidx.compose.ui.geometry.Size(5f, 7f)
                )
            }
        }
    }
}
