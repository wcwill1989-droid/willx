package com.willx.ai.matrix

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Sistema de responsividade para o efeito Matrix.
 * Adapta densidade, tamanho de glifos e outros parâmetros baseados no dispositivo e orientação.
 */
object MatrixResponsive {
    
    /**
     * Configuração responsiva baseada no tamanho da tela e densidade de pixels.
     */
    data class ResponsiveConfig(
        val columnCount: Int,
        val fontSize: Float,
        val densityMultiplier: Float,
        val performanceProfile: PerformanceProfile
    ) {
        enum class PerformanceProfile {
            HIGH,      // Dispositivos potentes
            MEDIUM,    // Dispositivos médios
            LOW,       // Dispositivos limitados ou bateria baixa
            ADAPTIVE   // Ajusta dinamicamente
        }
    }
    
    /**
     * Calcula configuração responsiva baseada no tamanho da tela e densidade.
     */
    @Composable
    fun calculateResponsiveConfig(
        screenSize: Size,
        config: MatrixConfig = MatrixConfig.Default
    ): ResponsiveConfig {
        val density = LocalDensity.current.density
        val configuration = LocalConfiguration.current
        
        val screenWidthDp = configuration.screenWidthDp.dp
        val screenHeightDp = configuration.screenHeightDp.dp
        val isLandscape = screenWidthDp > screenHeightDp
        
        return calculateResponsiveConfigNonComposable(
            screenSize = screenSize,
            config = config,
            density = density,
            screenWidthDp = screenWidthDp,
            screenHeightDp = screenHeightDp,
            isLandscape = isLandscape
        )
    }
    
    /**
     * Versão não-composable de calculateResponsiveConfig.
     */
    fun calculateResponsiveConfigNonComposable(
        screenSize: Size,
        config: MatrixConfig = MatrixConfig.Default,
        density: Float,
        screenWidthDp: Dp,
        screenHeightDp: Dp,
        isLandscape: Boolean
    ): ResponsiveConfig {
        // Determinar perfil de performance baseado em heurísticas
        val performanceProfile = determinePerformanceProfile(
            screenSize = screenSize,
            density = density,
            isLandscape = isLandscape
        )
        
        // Calcular número de colunas baseado na largura e densidade
        val baseColumnCount = calculateColumnCount(
            screenWidth = screenSize.width,
            density = density,
            isLandscape = isLandscape,
            baseDensity = config.density
        )
        
        // Calcular tamanho de fonte adaptativo
        val baseFontSize = calculateFontSize(density, isLandscape)
        
        // Ajustar multiplicador de densidade baseado no perfil de performance
        val densityMultiplier = when (performanceProfile) {
            ResponsiveConfig.PerformanceProfile.HIGH -> 1.2f
            ResponsiveConfig.PerformanceProfile.MEDIUM -> 1.0f
            ResponsiveConfig.PerformanceProfile.LOW -> 0.7f
            ResponsiveConfig.PerformanceProfile.ADAPTIVE -> 1.0f
        }
        
        return ResponsiveConfig(
            columnCount = baseColumnCount,
            fontSize = baseFontSize,
            densityMultiplier = densityMultiplier,
            performanceProfile = performanceProfile
        )
    }
    
    /**
     * Determina o perfil de performance ideal para o dispositivo atual.
     */
    private fun determinePerformanceProfile(
        screenSize: Size,
        density: Float,
        isLandscape: Boolean
    ): ResponsiveConfig.PerformanceProfile {
        val totalPixels = screenSize.width * screenSize.height
        
        // Heurísticas simples baseadas em resolução e densidade
        return when {
            // Dispositivos de alta performance (telas grandes com alta densidade)
            totalPixels > 2000000 && density > 2.5f -> 
                ResponsiveConfig.PerformanceProfile.HIGH
            
            // Dispositivos de baixa performance (telas pequenas ou baixa densidade)
            totalPixels < 800000 || density < 1.5f -> 
                ResponsiveConfig.PerformanceProfile.LOW
            
            // Perfil médio para a maioria dos dispositivos
            else -> ResponsiveConfig.PerformanceProfile.MEDIUM
        }
    }
    
    /**
     * Calcula número ideal de colunas baseado na largura da tela.
     */
    private fun calculateColumnCount(
        screenWidth: Float,
        density: Float,
        isLandscape: Boolean,
        baseDensity: Float
    ): Int {
        // Largura base por coluna em pixels (ajustada por densidade)
        val baseColumnWidth = 40f * density
        
        // Número base de colunas
        var columns = (screenWidth / baseColumnWidth).roundToInt()
        
        // Ajustar para orientação
        columns = if (isLandscape) {
            // Em paisagem, menos colunas para melhor performance
            (columns * 0.8f).roundToInt().coerceAtLeast(12)
        } else {
            // Em retrato, mais colunas para densidade visual
            columns.coerceIn(12, 36)
        }
        
        // Aplicar densidade configurada pelo usuário
        columns = (columns * baseDensity).roundToInt().coerceIn(8, 48)
        
        return columns
    }
    
    /**
     * Calcula tamanho de fonte adaptativo.
     */
    private fun calculateFontSize(density: Float, isLandscape: Boolean): Float {
        val baseSize = 14f * density
        
        return if (isLandscape) {
            // Em paisagem, fonte um pouco menor
            baseSize * 0.9f
        } else {
            baseSize
        }.coerceIn(12f * density, 24f * density)
    }
    
    /**
     * Cria uma MatrixConfig adaptada para o dispositivo atual.
     */
    fun createAdaptiveMatrixConfig(
        baseConfig: MatrixConfig = MatrixConfig.Default,
        screenWidthDp: Int,
        screenHeightDp: Int,
        density: Float,
        orientation: Int
    ): MatrixConfig {
        val isLandscape = orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        
        val responsiveConfig = calculateResponsiveConfigNonComposable(
            screenSize = Size(
                width = screenWidthDp * density,
                height = screenHeightDp * density
            ),
            config = baseConfig,
            density = density,
            screenWidthDp = screenWidthDp.dp,
            screenHeightDp = screenHeightDp.dp,
            isLandscape = isLandscape
        )
        
        // Ajustar configuração baseada no perfil de performance
        return when (responsiveConfig.performanceProfile) {
            ResponsiveConfig.PerformanceProfile.HIGH -> {
                baseConfig.copy(
                    density = baseConfig.density * 1.2f,
                    speed = baseConfig.speed * 1.1f,
                    trailLength = 15,
                    highlightChance = 0.07f
                )
            }
            ResponsiveConfig.PerformanceProfile.MEDIUM -> {
                baseConfig.copy(
                    density = baseConfig.density * 1.0f,
                    trailLength = 12,
                    highlightChance = 0.05f
                )
            }
            ResponsiveConfig.PerformanceProfile.LOW -> {
                baseConfig.copy(
                    density = baseConfig.density * 0.7f,
                    speed = baseConfig.speed * 0.8f,
                    trailLength = 8,
                    highlightChance = 0.03f,
                    brightness = baseConfig.brightness * 0.9f
                )
            }
            ResponsiveConfig.PerformanceProfile.ADAPTIVE -> {
                baseConfig
            }
        }
    }
    
    /**
     * Composable que observa mudanças de configuração e atualiza o efeito Matrix.
     */
    @Composable
    fun ResponsiveMatrixEffect(
        modifier: Modifier = androidx.compose.ui.Modifier,
        baseConfig: MatrixConfig = MatrixConfig.Default,
        onConfigAdapted: (MatrixConfig) -> Unit = {},
        content: @Composable (adaptedConfig: MatrixConfig) -> Unit
    ) {
        val configuration = LocalConfiguration.current
        val density = LocalDensity.current.density
        
        // Calcular configuração adaptada usando remember para evitar recomposições desnecessárias
        val adaptedConfig = remember(configuration, density, baseConfig) {
            createAdaptiveMatrixConfig(
                baseConfig = baseConfig,
                screenWidthDp = configuration.screenWidthDp,
                screenHeightDp = configuration.screenHeightDp,
                density = density,
                orientation = configuration.orientation
            )
        }
        
        // Notificar quando a configuração mudar
        LaunchedEffect(adaptedConfig) {
            onConfigAdapted(adaptedConfig)
        }
        
        // Conteúdo com configuração adaptada
        content(adaptedConfig)
    }
    
    /**
     * Extensão para redimensionamento suave do sistema de partículas.
     */
    fun MatrixParticleSystem.adaptToNewSize(newSize: Size, animate: Boolean = true) {
        val (newWidth, newHeight) = newSize.width to newSize.height
        
        if (animate) {
            // Implementação futura: transição suave
            onScreenSizeChanged(newWidth, newHeight)
        } else {
            onScreenSizeChanged(newWidth, newHeight)
        }
    }
}

/**
 * Composable de efeito Matrix com responsividade integrada.
 */
@Composable
fun ResponsiveMatrixEffect(
    modifier: Modifier = Modifier,
    baseConfig: MatrixConfig = MatrixConfig.Default,
    enabled: Boolean = true
) {
    MatrixResponsive.ResponsiveMatrixEffect(
        modifier = modifier,
        baseConfig = baseConfig
    ) { adaptedConfig ->
        MatrixEffect(
            modifier = modifier,
            config = adaptedConfig,
            enabled = enabled
        )
    }
}