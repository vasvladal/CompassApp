package com.example.compassapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.compassapp.compass.roundTo
import kotlin.math.abs

@Composable
expect fun MapView(
    latitude: Double,
    longitude: Double,
    modifier: Modifier = Modifier
)

@Composable
fun CoordinatesDisplay(
    latitude: Double,
    longitude: Double,
    modifier: Modifier = Modifier
) {
    val latVal = abs(latitude)
    val lonVal = abs(longitude)

    val latDeg = latVal.toInt()
    val latMin = ((latVal - latDeg) * 60).toInt()
    val latSec = ((latVal - latDeg) * 60 - latMin) * 60

    val lonDeg = lonVal.toInt()
    val lonMin = ((lonVal - lonDeg) * 60).toInt()
    val lonSec = ((lonVal - lonDeg) * 60 - lonMin) * 60

    val latDir = if (latitude >= 0) "N" else "S"
    val lonDir = if (longitude >= 0) "E" else "W"

    val Purple = Color(0xFFD0B7FF)
    val LightGray = Color(0xFF888888)
    val White = Color(0xFFF4F1FA)
    val DarkCard = Color(0xFF1A1A1A)

    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = "📍 Location Coordinates", color = Purple, fontSize = 14.sp, fontWeight = FontWeight.Bold)

            Text(text = "Latitude:", color = LightGray, fontSize = 12.sp)
            Text(
                text = "${latDeg}°${latMin.toString().padStart(2, '0')}'${latSec.roundTo(2).toString().padStart(5, '0')}\" $latDir",
                color = White, fontSize = 16.sp, fontWeight = FontWeight.Medium
            )
            Text(text = "${latitude.roundTo(6)}°", color = LightGray, fontSize = 13.sp)

            Spacer(Modifier.height(4.dp))

            Text(text = "Longitude:", color = LightGray, fontSize = 12.sp)
            Text(
                text = "${lonDeg}°${lonMin.toString().padStart(2, '0')}'${lonSec.roundTo(2).toString().padStart(5, '0')}\" $lonDir",
                color = White, fontSize = 16.sp, fontWeight = FontWeight.Medium
            )
            Text(text = "${longitude.roundTo(6)}°", color = LightGray, fontSize = 13.sp)
        }
    }
}