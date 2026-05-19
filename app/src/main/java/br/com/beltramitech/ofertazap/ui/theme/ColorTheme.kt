package br.com.beltramitech.ofertazap.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val OfertaZapColors = lightColorScheme(
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

@Composable
fun OfertaZapTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = OfertaZapColors,
        content = content
    )
}
