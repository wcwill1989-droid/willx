package com.willx.ai

import android.util.Log
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlin.math.floor
import kotlin.random.Random

@Composable
fun WillXTheme(
    mode: String,
    content: @Composable BoxScope.() -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val scheme = when (mode) {
        ThemeMode.DARK -> darkColorScheme()
        ThemeMode.MATRIX -> matrixColorScheme()
        else -> if (systemDark) darkColorScheme() else lightColorScheme()
    }

    val typography = MaterialTheme.typography.copy(
        bodyLarge = MaterialTheme.typography.bodyLarge.merge(TextStyle(fontFamily = FontFamily.Monospace)),
        bodyMedium = MaterialTheme.typography.bodyMedium.merge(TextStyle(fontFamily = FontFamily.Monospace)),
        bodySmall = MaterialTheme.typography.bodySmall.merge(TextStyle(fontFamily = FontFamily.Monospace)),
        titleLarge = MaterialTheme.typography.titleLarge.merge(TextStyle(fontFamily = FontFamily.Monospace)),
        titleMedium = MaterialTheme.typography.titleMedium.merge(TextStyle(fontFamily = FontFamily.Monospace)),
        labelSmall = MaterialTheme.typography.labelSmall.merge(TextStyle(fontFamily = FontFamily.Monospace)),
    )

    MaterialTheme(colorScheme = scheme, typography = typography) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (mode == ThemeMode.MATRIX) Color.Transparent 
                    else MaterialTheme.colorScheme.background
                )
        ) {
            // Renderize a chuva Matrix no fundo se o tema estiver ativo
            if (mode == ThemeMode.MATRIX) {
                MatrixRain(enabled = true)
            }

            // Conteúdo principal fica por cima
            Box(modifier = Modifier.fillMaxSize()) {
                content()
            }
        }
    }
}

private fun matrixColorScheme(): ColorScheme {
    val matrixGreen = Color(0xFF00FF41)
    val darkBackground = Color(0xFF000000)
    val darkSurface = Color(0xFF0D0D0D)
    val brightWhite = Color(0xFFFFFFFF)

    return darkColorScheme(
        primary = matrixGreen,
        secondary = Color(0xFF00A82E),
        tertiary = Color(0xFF007F24),
        background = darkBackground,
        surface = darkSurface,
        onPrimary = Color.Black,
        onSecondary = Color.Black,
        onBackground = brightWhite,
        onSurface = brightWhite,
        error = Color(0xFFFF5555),
        onError = Color.White,
    )
}




