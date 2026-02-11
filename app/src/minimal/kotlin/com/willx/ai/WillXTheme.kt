package com.willx.ai

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
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (mode == ThemeMode.MATRIX) {
                MatrixBackdrop()
            }
            content()
        }
    }
}

private fun matrixColorScheme(): ColorScheme {
    val bg = Color(0xFF001000)
    val green = Color(0xFF00FF7A)
    val green2 = Color(0xFF00C853)
    return darkColorScheme(
        primary = green,
        secondary = green2,
        tertiary = Color(0xFF1BFFB3),
        background = bg,
        surface = Color(0xFF001A0A),
        onPrimary = Color(0xFF001000),
        onSecondary = Color(0xFF001000),
        onBackground = Color(0xFFB9FFD9),
        onSurface = Color(0xFFB9FFD9),
        error = Color(0xFFFF4D4D),
        onError = Color.Black,
    )
}

@Composable
private fun MatrixBackdrop() {
    val gradient = Brush.verticalGradient(
        0f to Color(0xFF000A00),
        1f to Color(0xFF001A0A),
    )

    val t = rememberInfiniteTransition(label = "matrix")
    val scan by t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2500, easing = LinearEasing)),
        label = "scan"
    )
    val rain by t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing)),
        label = "rain"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient)
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            drawScanlines(scan)
            drawRain(rain)
        }

        // subtle vignette overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        0.0f to Color.Transparent,
                        1.0f to Color(0xCC000000),
                    )
                )
                .alpha(0.55f)
        )
    }
}

private fun DrawScope.drawScanlines(p: Float) {
    val lineColor = Color(0x2200FF7A)
    val step = 6f
    var y = 0f
    while (y < size.height) {
        drawRect(
            color = lineColor,
            topLeft = Offset(0f, y),
            size = androidx.compose.ui.geometry.Size(size.width, 1.5f)
        )
        y += step
    }

    val scanY = (p * size.height)
    drawRect(
        color = Color(0x3300FF7A),
        topLeft = Offset(0f, scanY),
        size = androidx.compose.ui.geometry.Size(size.width, 26f)
    )
}

private fun DrawScope.drawRain(p: Float) {
    val cols = 18
    val colW = size.width / cols

    val seed = Random(1337)
    // precompute per-column speed/phase
    val speeds = FloatArray(cols) { 0.35f + seed.nextFloat() * 1.25f }
    val phases = FloatArray(cols) { seed.nextFloat() }

    drawIntoCanvas { canvas ->
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.argb(190, 0, 255, 122)
            textSize = 18f
            typeface = android.graphics.Typeface.MONOSPACE
        }

        for (i in 0 until cols) {
            val x = i * colW + colW * 0.2f
            val t = (p * speeds[i] + phases[i]) % 1f
            val yHead = t * (size.height + 300f) - 150f

            // tail
            val tailLen = 10
            for (k in 0 until tailLen) {
                val y = yHead - k * 22f
                if (y < -50f || y > size.height + 50f) continue
                val alpha = (1f - (k / tailLen.toFloat()))
                paint.alpha = floor(220f * alpha).toInt().coerceIn(0, 255)
                val ch = ((33 + ((i * 7 + k * 13) % 90))).toChar().toString()
                canvas.nativeCanvas.drawText(ch, x, y, paint)
            }
        }
    }
}
