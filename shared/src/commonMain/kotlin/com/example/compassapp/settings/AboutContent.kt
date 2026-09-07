package com.example.compassapp.settings

import com.example.compassapp.shared.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * Loads AboutEn.html or AboutRu.html from compose resources based on the selected language.
 */
@OptIn(ExperimentalResourceApi::class)
suspend fun loadAboutHtml(language: Language): String {
    val fileName = when (language) {
        Language.ENGLISH -> "AboutEn.html"
        Language.RUSSIAN -> "AboutRu.html"
    }

    val html = runCatching {
        Res.readBytes("files/$fileName").decodeToString()
    }.getOrNull()

    if (html != null) return html

    // Last-resort fallback to built-in HTML if files are missing
    return aboutHtml(language)
}