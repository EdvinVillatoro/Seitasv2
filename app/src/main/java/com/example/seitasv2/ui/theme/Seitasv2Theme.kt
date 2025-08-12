package com.example.seitasv2.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val colorScheme = lightColorScheme(
    primary = OnPeach,
    onPrimary = ButtonWhite,
    surface = Peach,
    onSurface = OnPeach,
    background = Peach,
    onBackground = OnPeach
)

@Composable
fun Seitasv2Theme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
