package com.example.compassapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.compassapp.settings.AppSettings
import com.example.compassapp.settings.Language
//import com.example.compassapp.settings.aboutHtml
import com.example.compassapp.settings.stringsFor

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.LaunchedEffect
import com.example.compassapp.settings.loadAboutHtml



@Composable
private fun AboutPage(language: Language, modifier: Modifier = Modifier) {
    var html by remember(language) { mutableStateOf<String?>(null) }

    LaunchedEffect(language) {
        html = loadAboutHtml(language)
    }

    val content = html
    if (content != null) {
        HtmlView(html = content, modifier = modifier)
    } else {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("…", color = SLightGray, fontSize = 16.sp)
        }
    }
}

private val SBlack = Color(0xFF000000)
private val SWhite = Color(0xFFF4F1FA)
private val SPurple = Color(0xFFD0B7FF)
private val SLightGray = Color(0xFF888888)
private val SDarkGray = Color(0xFF444444)

private enum class SettingsPage { ABOUT, LANGUAGE }

@Composable
fun SettingsOverlay(onClose: () -> Unit) {
    var page by remember { mutableStateOf<SettingsPage?>(null) }
    val language by AppSettings.language.collectAsState()
    val s = stringsFor(language)

    Box2Content(onClose, page, { page = it }, s.about, s.language, s.settings, language)
}

@Composable
private fun Box2Content(
    onClose: () -> Unit,
    page: SettingsPage?,
    setPage: (SettingsPage?) -> Unit,
    aboutTitle: String,
    languageTitle: String,
    settingsTitle: String,
    language: Language
) {
    // FIX 1: Removed redundant qualifier name (androidx.compose.foundation.layout.Box -> Box)
    Box(modifier = Modifier.fillMaxSize().background(SBlack)) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (page != null) {
                    Text(
                        text = "←", color = SWhite, fontSize = 22.sp,
                        modifier = Modifier.clickable { setPage(null) }.padding(end = 12.dp, top = 4.dp, bottom = 4.dp)
                    )
                }
                Text(
                    text = when (page) {
                        null -> settingsTitle
                        SettingsPage.ABOUT -> aboutTitle
                        SettingsPage.LANGUAGE -> languageTitle
                    },
                    color = SPurple, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "✕", color = SWhite, fontSize = 20.sp,
                    modifier = Modifier.clickable { onClose() }.padding(4.dp)
                )
            }
            Spacer(Modifier.height(20.dp))

            when (page) {
                null -> {
                    SettingsRow(title = aboutTitle) { setPage(SettingsPage.ABOUT) }
                    SettingsRow(title = languageTitle, subtitle = language.displayName) { setPage(SettingsPage.LANGUAGE) }
                }
                // FIX 2: Call the AboutPage function properly and pass the modifier
                SettingsPage.ABOUT -> AboutPage(
                    language = language,
                    modifier = Modifier.weight(1f)
                )
                SettingsPage.LANGUAGE -> LanguagePage()
            }
        }
    }
}

@Composable
private fun SettingsRow(title: String, subtitle: String? = null, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SDarkGray.copy(alpha = 0.3f))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = SWhite, fontSize = 16.sp)
            if (subtitle != null) Text(subtitle, color = SLightGray, fontSize = 12.sp)
        }
        Text("›", color = SLightGray, fontSize = 20.sp)
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun LanguagePage() {
    val current by AppSettings.language.collectAsState()
    val s = stringsFor(current)

    Language.entries.forEach { lang ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SDarkGray.copy(alpha = 0.3f))
                .clickable { AppSettings.setLanguage(lang) }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(lang.displayName, color = SWhite, fontSize = 16.sp, modifier = Modifier.weight(1f))
            if (lang == current) Text("✓", color = SPurple, fontSize = 18.sp)
        }
        Spacer(Modifier.height(8.dp))
    }
    Spacer(Modifier.height(12.dp))
    Text(s.languageHint, color = SLightGray, fontSize = 12.sp)
}