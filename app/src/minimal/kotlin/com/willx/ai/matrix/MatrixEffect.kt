package com.willx.ai.matrix

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.floor
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.math.cos
import com.willx.ai.matrix.MatrixVisualEffects.drawEnhancedParticles

/**
 * Configuração principal do efeito Matrix.
 * Controla densidade, velocidade, cores e outros parâmetros visuais.
 */
data class MatrixConfig(
    val density: Float = 1.0f,           // 0.5 a 2.0
    val speed: Float = 1.0f,            // 0.5 a 3.0
    val brightness: Float = 1.0f,       // 0.5 a 2.0
    val highlightChance: Float = 0.05f, // Chance de glifo brilhante
    val trailLength: Int = 12,          // Comprimento do rastro
    val colorPrimary: Color = Color(0xFF00FF00),      // Verde neon brilhante
    val colorSecondary: Color = Color(0xFF00FFFF),    // Ciano
    val colorHighlight: Color = Color(0xFFFF00FF),    // Magenta
    val backgroundColor: Color = Color(0xFF000000),   // Preto puro
) {
    companion object {
        val Default = MatrixConfig()
        val HighDensity = MatrixConfig(density = 1.5f, speed = 1.2f)
        val LowPerformance = MatrixConfig(density = 0.7f, speed = 0.8f, trailLength = 8)
    }
}

/**
 * Representa um glifo individual na chuva Matrix.
 */
data class GlyphParticle(
    val columnIndex: Int,
    val char: Char,
    var positionY: Float,
    var velocity: Float,
    var opacity: Float,
    var brightness: Float,
    var isHighlight: Boolean,
    var age: Float = 0f,
    var maxAge: Float = 1.0f,
) {
    fun update(deltaTime: Float): Boolean {
        positionY += velocity * deltaTime
        age += deltaTime / maxAge
        opacity = 1f - age
        return age < 1f
    }
}



/**
 * Composable principal para o efeito Matrix.
 */
@Composable
fun MatrixEffect(
    modifier: Modifier = Modifier,
    config: MatrixConfig = MatrixConfig.Default,
    enabled: Boolean = true,
) {
    if (!enabled) {
        android.util.Log.d("MatrixEffect", "MatrixEffect disabled")
        return
    }
    
    android.util.Log.d("MatrixEffect", "MatrixEffect started with config: $config")
    
    val infiniteTransition = rememberInfiniteTransition(label = "matrix")
    val scanProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2500, easing = LinearEasing)),
        label = "scan"
    )
    
    // Sistema de partículas
    var particleSystem by remember { mutableStateOf<MatrixParticleSystem?>(null) }
    var frame by remember { mutableStateOf(0) }
    var time by remember { mutableStateOf(0f) }
    
    LaunchedEffect(config, enabled) {
        if (!enabled) return@LaunchedEffect
        
        android.util.Log.d("MatrixEffect", "MatrixEffect animation loop started")
        
        // Loop de animação
        while (enabled) {
            particleSystem?.update()
            frame++
            time += 0.016f // Incremento de tempo baseado em ~60 FPS
            kotlinx.coroutines.delay(16) // ~60 FPS
        }
        
        android.util.Log.d("MatrixEffect", "MatrixEffect animation loop ended")
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        val gradient = Brush.verticalGradient(
            0f to Color(0xFF000000),  // Preto no topo
            0.5f to Color(0xFF001100), // Verde muito escuro no meio
            1f to Color(0xFF002200),   // Verde um pouco mais claro embaixo
        )
        
        Canvas(
            modifier = Modifier.fillMaxSize(),
            onDraw = {
                // Fundo gradiente
                drawRect(brush = gradient, size = size)
                
                // 🔴 RETÂNGULO VERMELHO DE DEBUG NO CENTRO
                val centerX = size.width / 2
                val centerY = size.height / 2
                val debugRectSize = 100f
                drawRect(
                    color = Color.Red,
                    topLeft = Offset(centerX - debugRectSize / 2, centerY - debugRectSize / 2),
                    size = Size(debugRectSize, debugRectSize)
                )
                
                // Texto de debug
                drawIntoCanvas { canvas ->
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 24f
                        isAntiAlias = true
                    }
                    canvas.nativeCanvas.drawText(
                        "MATRIX DEBUG",
                        centerX - 80f,
                        centerY + 80f,
                        paint
                    )
                }
                
                // Efeito Matrix com sistema de partículas
                if (particleSystem == null) {
                    particleSystem = MatrixParticleSystem(config, size)
                } else {
                    // Usar efeitos visuais avançados
                    drawEnhancedParticles(
                        particleSystem = particleSystem!!,
                        config = config,
                        time = time
                    )
                }
                
                drawScanlines(size, scanProgress)
                drawVignette(size)
            }
        )
    }
}



private fun DrawScope.drawScanlines(size: Size, progress: Float) {
    val lineColor = Color(0x2200FF7A)
    val step = 6f
    var y = 0f
    
    while (y < size.height) {
        drawRect(
            color = lineColor,
            topLeft = Offset(0f, y),
            size = Size(size.width, 1.5f)
        )
        y += step
    }
    
    val scanY = progress * size.height
    drawRect(
        color = Color(0x3300FF7A),
        topLeft = Offset(0f, scanY),
        size = Size(size.width, 26f)
    )
}

private fun DrawScope.drawVignette(size: Size) {
    drawRect(
        brush = Brush.radialGradient(
            0.0f to Color.Transparent,
            1.0f to Color(0xCC000000),
        ),
        topLeft = Offset.Zero,
        size = size
    )
}