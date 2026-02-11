package com.willx.ai.matrix

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.floor
import kotlin.random.Random

/**
 * Sistema de partículas otimizado para o efeito Matrix.
 * Gerencia um pool de glifos com atualização baseada em tempo real.
 */
@Stable
class MatrixParticleSystem(
    private val config: MatrixConfig,
    screenSize: Size
) {
    private var screenWidth = screenSize.width
    private var screenHeight = screenSize.height
    
    private val random = Random(System.currentTimeMillis().toInt())
    private val particles = mutableListOf<GlyphParticle>()
    private var lastUpdateTime = System.currentTimeMillis()
    
    // Caracteres disponíveis para a chuva Matrix
    private val charSet = buildCharSet()
    
    // Configuração de colunas
    private var columnCount = calculateColumnCount()
    private var columnWidth = screenWidth / columnCount
    private val columnSpeeds = FloatArray(columnCount) { 
        0.5f + random.nextFloat() * 2.0f 
    }
    private val columnOffsets = FloatArray(columnCount) { 
        random.nextFloat() 
    }
    
    // Estatísticas
    private var _activeParticleCount: Int = 0
    val activeParticleCount: Int get() = _activeParticleCount
    private var _frameTimeMs: Long = 0
    val frameTimeMs: Long get() = _frameTimeMs
    
    init {
        initializeParticles()
    }
    
    private fun buildCharSet(): List<Char> {
        val chars = mutableListOf<Char>()
        
        // ASCII básico (33-126)
        for (i in 33..126) {
            chars.add(i.toChar())
        }
        
        // Katakana (para autenticidade Matrix)
        for (i in 0x30A0..0x30FF) {
            chars.add(i.toChar())
        }
        
        // Símbolos especiais
        val specialSymbols = listOf('▄', '▀', '█', '■', '▓', '▒', '░', '◘', '○', '●')
        chars.addAll(specialSymbols)
        
        // Números binários (ênfase)
        chars.addAll(listOf('0', '1'))
        
        return chars
    }
    
    private fun calculateColumnCount(): Int {
        val baseColumns = 18
        return (baseColumns * config.density).toInt().coerceIn(12, 36)
    }
    
    private fun initializeParticles() {
        particles.clear()
        
        for (i in 0 until columnCount) {
            val columnX = i * columnWidth + columnWidth * 0.2f
            val initialY = -random.nextFloat() * screenHeight
            
            // Criar rastro inicial para cada coluna
            for (j in 0 until config.trailLength) {
                val y = initialY - j * 22f
                if (y < -50f) continue
                
                particles.add(createParticle(i, columnX, y, j))
            }
        }
        
        _activeParticleCount = particles.size
    }
    
    private fun createParticle(
        columnIndex: Int,
        x: Float,
        y: Float,
        trailIndex: Int = 0
    ): GlyphParticle {
        val isHighlight = random.nextFloat() < config.highlightChance
        val char = charSet.random(random)
        val velocity = columnSpeeds[columnIndex] * config.speed * 60f
        val opacity = 1f - (trailIndex.toFloat() / config.trailLength)
        val brightness = if (isHighlight) 2.0f else 1.0f
        
        return GlyphParticle(
            columnIndex = columnIndex,
            char = char,
            positionY = y,
            velocity = velocity,
            opacity = opacity,
            brightness = brightness,
            isHighlight = isHighlight,
            maxAge = 1.0f + random.nextFloat() * 2.0f
        )
    }
    
    fun update(): Boolean {
        val startTime = System.currentTimeMillis()
        val currentTime = startTime
        val deltaTime = (currentTime - lastUpdateTime) / 1000f
        lastUpdateTime = currentTime
        
        if (deltaTime <= 0f) return false
        
        // Atualizar partículas existentes
        val iterator = particles.iterator()
        var updatedCount = 0
        
        while (iterator.hasNext()) {
            val particle = iterator.next()
            if (!particle.update(deltaTime)) {
                iterator.remove()
            } else {
                updatedCount++
            }
        }
        
        // Gerar novas partículas no topo
        val spawnChance = 0.02f * config.density * deltaTime * 60f
        for (i in 0 until columnCount) {
            if (random.nextFloat() < spawnChance) {
                val columnX = i * columnWidth + columnWidth * 0.2f
                val initialY = -50f
                particles.add(createParticle(i, columnX, initialY))
                updatedCount++
            }
        }
        
        // Remover partículas que saíram da tela
        particles.removeAll { it.positionY > screenHeight + 100f }
        
        _activeParticleCount = particles.size
        _frameTimeMs = System.currentTimeMillis() - startTime
        
        return updatedCount > 0
    }
    
    fun getParticles(): List<GlyphParticle> = particles.toList()
    
    fun getColumnWidth(): Float = columnWidth
    
    fun getColumnCount(): Int = columnCount
    
    fun getScreenSize(): Pair<Float, Float> = screenWidth to screenHeight
    
    fun onScreenSizeChanged(newWidth: Float, newHeight: Float) {
        if (newWidth <= 0f || newHeight <= 0f) return
        
        screenWidth = newWidth
        screenHeight = newHeight
        
        // Recalcular colunas
        columnCount = calculateColumnCount()
        columnWidth = screenWidth / columnCount
        
        // Reajustar partículas existentes
        val scaleX = newWidth / screenWidth
        particles.forEach { particle ->
            // Ajustar posição X baseada na nova largura da coluna
            val oldColumnX = particle.columnIndex * columnWidth
            particle.positionY = particle.positionY * (newHeight / screenHeight)
        }
    }
    
    fun drawParticles(drawScope: DrawScope) {
        drawScope.drawIntoCanvas { canvas ->
            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
                textSize = 18f * config.brightness
                typeface = android.graphics.Typeface.MONOSPACE
            }
            
            for (particle in particles) {
                val x = particle.columnIndex * columnWidth + columnWidth * 0.2f
                val y = particle.positionY
                
                // Configurar cor baseada no tipo de partícula
                if (particle.isHighlight) {
                    paint.color = android.graphics.Color.argb(
                        (255 * particle.opacity).toInt(),
                        185,  // R
                        255,  // G  
                        217   // B
                    )
                } else {
                    paint.color = android.graphics.Color.argb(
                        (190 * particle.opacity).toInt(),
                        0,    // R
                        255,  // G
                        122   // B
                    )
                }
                
                // Aplicar brilho
                paint.alpha = (paint.alpha * particle.brightness).toInt().coerceIn(0, 255)
                
                canvas.nativeCanvas.drawText(particle.char.toString(), x, y, paint)
            }
        }
    }
    
    fun reset() {
        particles.clear()
        initializeParticles()
    }
}