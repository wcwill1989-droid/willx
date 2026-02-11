package com.willx.ai.matrix

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * Sistema de otimização de performance para o efeito Matrix.
 * Monitora e ajusta parâmetros para garantir performance suave.
 */
object MatrixPerformance {
    
    /**
     * Perfil de performance baseado no dispositivo.
     */
    enum class PerformanceProfile {
        HIGH,      // Dispositivos potentes (Snapdragon 8xx, Apple A15+)
        MEDIUM,    // Dispositivos médios (Snapdragon 7xx, Apple A12-A14)
        LOW,       // Dispositivos limitados (Snapdragon 4xx, entry-level)
        ADAPTIVE   // Ajusta dinamicamente baseado em métricas
    }
    
    /**
     * Métricas de performance coletadas em tempo real.
     */
    data class PerformanceMetrics(
        val frameTimeMs: Float = 0f,
        val particleCount: Int = 0,
        val fps: Float = 0f,
        val memoryUsageMB: Float = 0f,
        val isSmooth: Boolean = true
    ) {
        companion object {
            val Empty = PerformanceMetrics()
        }
    }
    
    /**
     * Otimizador que ajusta parâmetros baseado em métricas de performance.
     */
    class PerformanceOptimizer(
        private val initialConfig: MatrixConfig = MatrixConfig.Default
    ) {
        private var currentConfig = initialConfig
        private var metricsHistory = mutableListOf<PerformanceMetrics>()
        private val maxHistorySize = 60 // Mantém 1 segundo de histórico a 60 FPS
        
        /**
         * Atualiza métricas e ajusta configuração se necessário.
         */
        fun updateMetrics(metrics: PerformanceMetrics): MatrixConfig {
            // Adicionar ao histórico
            metricsHistory.add(metrics)
            if (metricsHistory.size > maxHistorySize) {
                metricsHistory.removeAt(0)
            }
            
            // Verificar se precisa otimizar
            if (shouldOptimize(metrics)) {
                currentConfig = optimizeConfig(currentConfig, metrics)
            }
            
            return currentConfig
        }
        
        /**
         * Determina se a otimização é necessária.
         */
        private fun shouldOptimize(metrics: PerformanceMetrics): Boolean {
            // Otimizar se FPS estiver abaixo de 30
            if (metrics.fps < 30f) return true
            
            // Otimizar se tempo de frame estiver acima de 33ms (menos de 30 FPS)
            if (metrics.frameTimeMs > 33f) return true
            
            // Otimizar se não estiver suave
            if (!metrics.isSmooth) return true
            
            return false
        }
        
        /**
         * Otimiza configuração baseada nas métricas.
         */
        private fun optimizeConfig(config: MatrixConfig, metrics: PerformanceMetrics): MatrixConfig {
            val optimizationFactor = calculateOptimizationFactor(metrics)
            
            return config.copy(
                density = (config.density * optimizationFactor).coerceIn(0.5f, 3.0f),
                trailLength = (config.trailLength * optimizationFactor).toInt().coerceIn(1, 20),
                highlightChance = (config.highlightChance * optimizationFactor).coerceIn(0f, 0.1f),
                brightness = (config.brightness * optimizationFactor).coerceIn(0.5f, 1.5f)
            )
        }
        
        /**
         * Calcula fator de otimização (0.5 = reduzir pela metade, 1.0 = manter).
         */
        private fun calculateOptimizationFactor(metrics: PerformanceMetrics): Float {
            // Baseado no FPS
            return when {
                metrics.fps < 20f -> 0.5f  // Reduzir drasticamente
                metrics.fps < 30f -> 0.7f  // Reduzir significativamente
                metrics.fps < 45f -> 0.85f // Reduzir levemente
                else -> 1.0f               // Manter
            }
        }
        
        /**
         * Reseta para configuração inicial.
         */
        fun reset() {
            currentConfig = initialConfig
            metricsHistory.clear()
        }
        
        /**
         * Obtém métricas agregadas do histórico.
         */
        fun getAggregatedMetrics(): PerformanceMetrics {
            if (metricsHistory.isEmpty()) return PerformanceMetrics.Empty
            
            val avgFrameTime = metricsHistory.map { it.frameTimeMs }.average().toFloat()
            val avgFps = metricsHistory.map { it.fps }.average().toFloat()
            val avgParticleCount = metricsHistory.map { it.particleCount }.average().toFloat().roundToInt()
            val avgMemory = metricsHistory.map { it.memoryUsageMB }.average().toFloat()
            
            // Determinar se é suave (95% dos frames abaixo de 33ms)
            val smoothFrames = metricsHistory.count { it.frameTimeMs <= 33f }
            val smoothPercentage = smoothFrames.toFloat() / metricsHistory.size
            
            return PerformanceMetrics(
                frameTimeMs = avgFrameTime,
                particleCount = avgParticleCount,
                fps = avgFps,
                memoryUsageMB = avgMemory,
                isSmooth = smoothPercentage >= 0.95f
            )
        }
    }
    
    /**
     * Composable que monitora performance e ajusta dinamicamente.
     */
    @Composable
    fun AdaptiveMatrixEffect(
        modifier: Modifier = Modifier,
        initialConfig: MatrixConfig = MatrixConfig.Default,
        showMetrics: Boolean = false
    ) {
        val optimizer = remember { PerformanceOptimizer(initialConfig) }
        var currentConfig by remember { mutableStateOf(initialConfig) }
        var metrics by remember { mutableStateOf(PerformanceMetrics.Empty) }
        
        // Simular atualização de métricas (em implementação real, coletaria métricas reais)
        LaunchedEffect(Unit) {
            // Em produção, isso seria substituído por coleta real de métricas
            // Por enquanto, simulamos métricas estáveis
            metrics = PerformanceMetrics(
                frameTimeMs = 16.7f, // ~60 FPS
                particleCount = 100, // Valor simulado
                fps = 60f,
                memoryUsageMB = 50f,
                isSmooth = true
            )
        }
        
        // Atualizar configuração baseada em métricas
        LaunchedEffect(metrics) {
            val optimizedConfig = optimizer.updateMetrics(metrics)
            if (optimizedConfig != currentConfig) {
                currentConfig = optimizedConfig
            }
        }
        
        Box(modifier = modifier.fillMaxSize()) {
            // Efeito Matrix com configuração otimizada
            MatrixEffect(
                modifier = Modifier.fillMaxSize(),
                config = currentConfig,
                enabled = true
            )
            
            // Exibir métricas se habilitado
            if (showMetrics) {
                PerformanceMetricsDisplay(
                    metrics = metrics,
                    config = currentConfig,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                )
            }
        }
    }
    
    /**
     * Composable que exibe métricas de performance.
     */
    @Composable
    private fun PerformanceMetricsDisplay(
        metrics: PerformanceMetrics,
        config: MatrixConfig,
        modifier: Modifier = Modifier
    ) {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.7f),
                contentColor = Color.Green
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Performance",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Green
                )
                
                MetricRow(
                    label = "FPS",
                    value = "%.1f".format(metrics.fps),
                    isGood = metrics.fps >= 50f,
                    isWarning = metrics.fps in 30f..49f
                )
                
                MetricRow(
                    label = "Frame Time",
                    value = "%.1f ms".format(metrics.frameTimeMs),
                    isGood = metrics.frameTimeMs <= 20f,
                    isWarning = metrics.frameTimeMs in 21f..33f
                )
                
                MetricRow(
                    label = "Partículas",
                    value = "${metrics.particleCount}",
                    isGood = metrics.particleCount <= 150,
                    isWarning = metrics.particleCount in 151..250
                )
                
                MetricRow(
                    label = "Memória",
                    value = "%.1f MB".format(metrics.memoryUsageMB),
                    isGood = metrics.memoryUsageMB <= 100f,
                    isWarning = metrics.memoryUsageMB in 101f..200f
                )
                
                Divider(
                    color = Color.Green.copy(alpha = 0.3f),
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                
                Text(
                    text = "Configuração Atual",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Green.copy(alpha = 0.8f)
                )
                
                MetricRow(
                    label = "Densidade",
                    value = "%.2f".format(config.density),
                    isGood = true,
                    isWarning = false
                )
                
                MetricRow(
                    label = "Velocidade",
                    value = "%.2f".format(config.speed),
                    isGood = true,
                    isWarning = false
                )
                
                Text(
                    text = if (metrics.isSmooth) "✓ Suave" else "⚠️ Otimizando",
                    fontSize = 11.sp,
                    color = if (metrics.isSmooth) Color.Green else Color.Yellow,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
    
    /**
     * Linha de métrica individual.
     */
    @Composable
    private fun MetricRow(
        label: String,
        value: String,
        isGood: Boolean,
        isWarning: Boolean
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color.Green.copy(alpha = 0.8f)
            )
            
            Text(
                text = value,
                fontSize = 12.sp,
                color = when {
                    isGood -> Color.Green
                    isWarning -> Color.Yellow
                    else -> Color.Red
                },
                fontWeight = if (isGood) FontWeight.Normal else FontWeight.Bold
            )
        }
    }
    
    /**
     * Sugestões de otimização baseadas no perfil do dispositivo.
     */
    fun getOptimizationSuggestions(profile: PerformanceProfile): List<String> {
        return when (profile) {
            PerformanceProfile.HIGH -> listOf(
                "Aumentar densidade para efeito mais denso",
                "Aumentar comprimento do rastro para mais realismo",
                "Aumentar chance de destaque para mais brilho"
            )
            PerformanceProfile.MEDIUM -> listOf(
                "Manter densidade moderada",
                "Usar rastro médio (8-12 quadros)",
                "Manter brilho padrão"
            )
            PerformanceProfile.LOW -> listOf(
                "Reduzir densidade para melhor performance",
                "Reduzir comprimento do rastro para 4-8 quadros",
                "Reduzir número de partículas",
                "Desativar efeitos avançados se necessário"
            )
            PerformanceProfile.ADAPTIVE -> listOf(
                "Ajustar dinamicamente baseado em FPS",
                "Monitorar temperatura do dispositivo",
                "Reduzir qualidade quando bateria estiver baixa"
            )
        }
    }
}