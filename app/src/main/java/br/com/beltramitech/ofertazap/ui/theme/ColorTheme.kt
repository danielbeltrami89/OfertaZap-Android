package dev.beltramitech.ofertazap.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val OfertaZapLightColors = lightColorScheme(
    primary = Color(0xFF0A84FF),
    secondary = Color(0xFF34C759),
    background = Color.White,
    surface = Color.White,
    surfaceVariant = Color(0xFFF2F2F7),
    onPrimary = Color.White,
    onBackground = Color(0xFF1C1C1E),
    onSurface = Color(0xFF1C1C1E),
    onSurfaceVariant = Color(0xFF636366)
)

private val OfertaZapDarkColors = darkColorScheme(
    primary = Color(0xFF0A84FF),
    secondary = Color(0xFF34C759),
    background = Color(0xFF101014),
    surface = Color(0xFF1C1C1E),
    surfaceVariant = Color(0xFF2C2C2E),
    onPrimary = Color.White,
    onBackground = Color(0xFFF7F7FA),
    onSurface = Color(0xFFF7F7FA),
    onSurfaceVariant = Color(0xFFC7C7CC)
)

@Composable
fun OfertaZapTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) OfertaZapDarkColors else OfertaZapLightColors,
        content = content
    )
}
