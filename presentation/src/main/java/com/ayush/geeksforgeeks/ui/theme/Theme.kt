package com.ayush.geeksforgeeks.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = GFGPrimary,
    secondary = GFGSecondary,
    background = GFGBackground,
    surface = GFGCardBackground,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = GFGTextPrimary,
    onSurface = GFGTextPrimary
)

@Composable
fun GFGGGVTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}