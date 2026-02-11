package com.willx.ai

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
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.floor
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.delay

/**
 * Efeito Matrix - Chuva digital icônica (AGORA FUNCIONANDO!)
 */
@Composable
fun MatrixRain(
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    if (!enabled) return
    
    // Sistema de partículas otimizado
    data class RainDrop(
        val column: Int,
        var position: Float,
        val speed: Float,
        val length: Int,
        val chars: List<Char>,
        var brightness: Float = 1f
    )
    
    var drops by remember { mutableStateOf<List<RainDrop>>(emptyList()) }
    var columns by remember { mutableStateOf(0) }
    val charWidth = 18f
    val charHeight = 22f
    
    // Caracteres Matrix autênticos
    val matrixChars = "01アイウエオカキクケコサシスセソタチツテトナニヌネノハヒフヘホマミムメモヤユヨラリルレロワヲン"
    val binaryChars = "01"
    
    // Inicializar sistema
    LaunchedEffect(enabled) {
        if (!enabled) return@LaunchedEffect
        
        // Loop de animação
        while (enabled) {
            // Atualizar gotas
            drops = drops.map { drop ->
                val newPosition = drop.position + drop.speed
                val newBrightness = kotlin.math.max(0.2f, 1f - (newPosition / (charHeight * 25)))
                
                drop.copy(
                    position = newPosition,
                    brightness = newBrightness
                )
            }.filter { it.position < charHeight * 40 }
            
            // Adicionar novas gotas
            if (columns > 0 && Random.nextFloat() < 0.4f) {
                val newColumn = Random.nextInt(columns)
                val newLength = 8 + Random.nextInt(20)
                val newChars = List(newLength) { 
                    if (Random.nextBoolean()) 
                        matrixChars[Random.nextInt(matrixChars.length)]
                    else
                        binaryChars[Random.nextInt(binaryChars.length)]
                }
                
                drops = drops + RainDrop(
                    column = newColumn,
                    position = -charHeight * newLength,
                    speed = 1.5f + Random.nextFloat() * 4f,
                    length = newLength,
                    chars = newChars
                )
            }
            
            delay(16) // ~60 FPS
        }
    }
    
    // Transição para scanlines
    val infiniteTransition = rememberInfiniteTransition(label = "matrix")
    val scanProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3500, easing = LinearEasing)),
        label = "scan"
    )
    
    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier.fillMaxSize(),
            onDraw = {
                val width = size.width
                val height = size.height
                
                // Calcular colunas
                columns = floor(width / charWidth).toInt()
                
                // 🔴 FUNDO MATRIX - Verde SEMI-TRANSPARENTE sobre conteúdo
                drawRect(
                    color = Color(0x88006600), // Verde claro 50% transparente
                    size = size
                )
                
                // Gradiente verde sutil
                val gradient = Brush.verticalGradient(
                    colors = listOf(
                        Color(0x3300FF00), // Verde transparente no topo
                        Color(0x1100AA00),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = height * 0.7f
                )
                drawRect(brush = gradient, size = size)
                
                // Inicializar gotas se necessário
                if (drops.isEmpty() && columns > 0) {
                    drops = List(columns / 2) { // Metade das colunas preenchidas
                        val column = Random.nextInt(columns)
                        val length = 8 + Random.nextInt(20)
                        RainDrop(
                            column = column,
                            position = Random.nextFloat() * -height * 0.5f,
                            speed = 1.5f + Random.nextFloat() * 4f,
                            length = length,
                            chars = List(length) { 
                                matrixChars[Random.nextInt(matrixChars.length)]
                            }
                        )
                    }
                }
                
                // Desenhar gotas (CHUVA DIGITAL)
                drops.forEach { drop ->
                    val x = drop.column * charWidth + charWidth / 2
                    
                    drop.chars.forEachIndexed { index, char ->
                        val charY = drop.position + (index * charHeight)
                        
                        if (charY in -charHeight..height) {
                            val positionInDrop = index.toFloat() / drop.chars.size
                            val brightness = drop.brightness * (1f - positionInDrop * 0.6f)
                            
                            // Cor Matrix (verde com gradiente)
                            val greenIntensity = when {
                                index == 0 -> 1f // Primeiro caractere BRANCO
                                brightness > 0.8f -> 0.9f
                                brightness > 0.6f -> 0.7f
                                brightness > 0.4f -> 0.5f
                                else -> 0.3f
                            }
                            
                            val isBright = index == 0 || Random.nextFloat() < 0.1f
                            val finalGreen = if (isBright) 1f else greenIntensity
                            val finalRed = if (isBright) 1f else 0f
                            
                            drawIntoCanvas { canvas ->
                                val paint = android.graphics.Paint().apply {
                                    this.color = android.graphics.Color.argb(
                                        (brightness * 255).toInt(),
                                        (finalRed * 255).toInt(),
                                        (finalGreen * 255).toInt(),
                                        0
                                    )
                                    textSize = charHeight
                                    isAntiAlias = true
                                    typeface = android.graphics.Typeface.MONOSPACE
                                }
                                canvas.nativeCanvas.drawText(
                                    char.toString(),
                                    x,
                                    charY,
                                    paint
                                )
                            }
                        }
                    }
                }
                
                // Scanlines animadas
                val lineColor = Color(0x3300FF00)
                val step = 2.5f
                var y = 0f
                
                while (y < height) {
                    val alpha = 0.1f + 0.1f * sin(y / 80f + scanProgress * 2f * kotlin.math.PI.toFloat())
                    drawRect(
                        color = lineColor.copy(alpha = alpha),
                        topLeft = Offset(0f, y),
                        size = Size(width, 1f)
                    )
                    y += step
                }
                
                // Vignette (escurecimento nas bordas)
                val vignette = Brush.radialGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xAA000000)
                    ),
                    center = Offset(width / 2, height / 2),
                    radius = width * 0.6f
                )
                drawRect(brush = vignette, size = size)
                

            }
        )
    }
}