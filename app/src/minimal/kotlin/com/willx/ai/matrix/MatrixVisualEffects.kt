package com.willx.ai.matrix

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Renderizador de efeitos visuais avançados para o efeito Matrix.
 */
object MatrixVisualEffects {
    
    /**
     * Desenha um rastro suave com desvanecimento exponencial.
     */
    fun DrawScope.drawSmoothTrail(
        particles: List<GlyphParticle>,
        columnWidth: Float,
        config: MatrixConfig
    ) {
        drawIntoCanvas { canvas ->
            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
                typeface = android.graphics.Typeface.MONOSPACE
            }
            
            // Agrupar partículas por coluna para renderização eficiente
            val particlesByColumn = particles.groupBy { it.columnIndex }
            
            for ((columnIndex, columnParticles) in particlesByColumn) {
                val x = columnIndex * columnWidth + columnWidth * 0.2f
                
                // Ordenar por posição Y (do topo para baixo)
                val sortedParticles = columnParticles.sortedBy { it.positionY }
                
                // Desenhar com efeito de rastro contínuo
                for ((index, particle) in sortedParticles.withIndex()) {
                    val y = particle.positionY
                    
                    // Efeito de desvanecimento exponencial
                    val trailFactor = exp(-index * 0.3f).coerceIn(0.1f, 1.0f)
                    val alpha = (particle.opacity * trailFactor * 255).toInt().coerceIn(0, 255)
                    
                    // Configurar cor baseada no tipo de partícula
                    if (particle.isHighlight) {
                        // Efeito de brilho para destaque
                        paint.color = android.graphics.Color.argb(
                            alpha,
                            (185 * particle.brightness).toInt().coerceIn(0, 255),
                            (255 * particle.brightness).toInt().coerceIn(0, 255),
                            (217 * particle.brightness).toInt().coerceIn(0, 255)
                        )
                        
                        // Adicionar glow effect para partículas destacadas
                        if (particle.brightness > 1.5f) {
                            val glowPaint = android.graphics.Paint(paint).apply {
                                color = android.graphics.Color.argb(
                                    (alpha * 0.3f).toInt(),
                                    185,
                                    255,
                                    217
                                )
                                textSize = paint.textSize * 1.3f
                            }
                            canvas.nativeCanvas.drawText(
                                particle.char.toString(),
                                x,
                                y,
                                glowPaint
                            )
                        }
                    } else {
                        // Cor normal Matrix
                        paint.color = android.graphics.Color.argb(
                            alpha,
                            0,
                            255,
                            122
                        )
                    }
                    
                    paint.textSize = 18f * config.brightness * particle.brightness
                    
                    // Efeito de distorção ocasional (glitch)
                    val drawX = if (Random.nextFloat() < 0.01f) {
                        x + Random.nextFloat() * 4f - 2f
                    } else {
                        x
                    }
                    
                    canvas.nativeCanvas.drawText(particle.char.toString(), drawX, y, paint)
                    
                    // Efeito de "ping" para partículas recém-criadas
                    if (particle.age < 0.1f) {
                        val pingAlpha = (alpha * (1f - particle.age * 10f)).toInt()
                        val pingPaint = android.graphics.Paint(paint).apply {
                            this.alpha = pingAlpha
                            textSize = paint.textSize * 1.5f
                        }
                        canvas.nativeCanvas.drawText(
                            particle.char.toString(),
                            x,
                            y,
                            pingPaint
                        )
                    }
                }
            }
        }
    }
    
    /**
     * Desenha efeitos de conexão entre partículas próximas.
     */
    fun DrawScope.drawParticleConnections(
        particles: List<GlyphParticle>,
        columnWidth: Float,
        maxDistance: Float = 40f
    ) {
        if (particles.size < 2) return
        
        drawIntoCanvas { canvas ->
            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.argb(40, 0, 255, 122)
                strokeWidth = 1f
                style = android.graphics.Paint.Style.STROKE
            }
            
            // Verificar conexões entre partículas próximas
            for (i in particles.indices) {
                for (j in i + 1 until particles.size) {
                    val p1 = particles[i]
                    val p2 = particles[j]
                    
                    val x1 = p1.columnIndex * columnWidth + columnWidth * 0.2f
                    val x2 = p2.columnIndex * columnWidth + columnWidth * 0.2f
                    
                    val dx = x2 - x1
                    val dy = p2.positionY - p1.positionY
                    val distance = kotlin.math.sqrt(dx * dx + dy * dy)
                    
                    if (distance < maxDistance && Random.nextFloat() < 0.3f) {
                        val alpha = ((1f - distance / maxDistance) * 40).toInt()
                        paint.alpha = alpha
                        
                        canvas.nativeCanvas.drawLine(x1, p1.positionY, x2, p2.positionY, paint)
                    }
                }
            }
        }
    }
    
    /**
     * Desenha efeitos de "onda" que se propagam horizontalmente.
     */
    fun DrawScope.drawWaveEffect(
        time: Float,
        size: Size,
        intensity: Float = 0.5f
    ) {
        val waveCount = 3
        val waveHeight = 20f * intensity
        
        drawIntoCanvas { canvas ->
            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.argb(30, 0, 255, 122)
                strokeWidth = 2f
                style = android.graphics.Paint.Style.STROKE
            }
            
            for (i in 0 until waveCount) {
                val phase = time * 0.5f + i * 0.7f
                val waveY = size.height * 0.5f + sin(phase) * waveHeight
                
                // Linha de onda
                canvas.nativeCanvas.drawLine(0f, waveY, size.width, waveY, paint)
                
                // Pontos de intensidade ao longo da onda
                val pointPaint = android.graphics.Paint(paint).apply {
                    style = android.graphics.Paint.Style.FILL
                    alpha = 60
                }
                
                for (x in 0 until size.width.toInt() step 50) {
                    val pointPhase = phase + x * 0.01f
                    val pointY = waveY + sin(pointPhase) * 10f
                    canvas.nativeCanvas.drawCircle(x.toFloat(), pointY, 2f, pointPaint)
                }
            }
        }
    }
    
    /**
     * Desenha efeitos de "glitch" aleatórios (distorção digital).
     */
    fun DrawScope.drawGlitchEffect(
        time: Float,
        size: Size,
        glitchChance: Float = 0.005f
    ) {
        if (Random.nextFloat() > glitchChance) return
        
        drawIntoCanvas { canvas ->
            val glitchType = Random.nextInt(3)
            val paint = android.graphics.Paint().apply {
                isAntiAlias = false
                color = android.graphics.Color.argb(100, 255, 0, 0)
            }
            
            when (glitchType) {
                0 -> {
                    // Glitch horizontal
                    val y = Random.nextFloat() * size.height
                    val height = Random.nextFloat() * 10f + 2f
                    canvas.nativeCanvas.drawRect(
                        0f, y, size.width, y + height, paint
                    )
                }
                1 -> {
                    // Glitch vertical
                    val x = Random.nextFloat() * size.width
                    val width = Random.nextFloat() * 20f + 5f
                    canvas.nativeCanvas.drawRect(
                        x, 0f, x + width, size.height, paint
                    )
                }
                2 -> {
                    // Glitch de bloco
                    val x = Random.nextFloat() * size.width
                    val y = Random.nextFloat() * size.height
                    val size = Random.nextFloat() * 50f + 20f
                    canvas.nativeCanvas.drawRect(
                        x, y, x + size, y + size, paint
                    )
                }
            }
        }
    }
    
    /**
     * Desenha partículas com efeitos visuais avançados.
     */
    fun DrawScope.drawEnhancedParticles(
        particleSystem: MatrixParticleSystem,
        config: MatrixConfig,
        time: Float
    ) {
        val particles = particleSystem.getParticles()
        val columnWidth = particleSystem.getColumnWidth()
        
        // 1. Desenhar conexões entre partículas
        if (config.density > 0.8f) {
            drawParticleConnections(particles, columnWidth)
        }
        
        // 2. Desenhar rastro suave
        drawSmoothTrail(particles, columnWidth, config)
        
        // 3. Efeitos de onda (ocasionais)
        if (Random.nextFloat() < 0.01f) {
            drawWaveEffect(time, size, intensity = 0.3f)
        }
        
        // 4. Efeitos de glitch
        drawGlitchEffect(time, size, glitchChance = 0.003f)
    }
}

