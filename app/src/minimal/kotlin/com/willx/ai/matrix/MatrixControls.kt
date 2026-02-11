package com.willx.ai.matrix

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Sistema de controles interativos para o efeito Matrix.
 * Permite ajustar parâmetros em tempo real.
 */
object MatrixControls {
    
    /**
     * Composable que exibe controles flutuantes para ajustar o efeito Matrix.
     */
    @Composable
    fun MatrixControlPanel(
        modifier: Modifier = Modifier,
        config: MatrixConfig,
        onConfigChanged: (MatrixConfig) -> Unit,
        onToggleVisibility: () -> Unit = {},
        onReset: () -> Unit = {}
    ) {
        var showAdvanced by remember { mutableStateOf(false) }
        
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.8f),
                contentColor = Color.Green
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Cabeçalho
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Controles Matrix",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Green
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { showAdvanced = !showAdvanced },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = Color.Green.copy(alpha = 0.2f),
                                contentColor = Color.Green
                            )
                        ) {
                            Icon(
                                imageVector = if (showAdvanced) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (showAdvanced) "Mostrar menos" else "Mostrar mais"
                            )
                        }
                        
                        IconButton(
                            onClick = onToggleVisibility,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = Color.Green.copy(alpha = 0.2f),
                                contentColor = Color.Green
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = "Alternar visibilidade"
                            )
                        }
                    }
                }
                
                // Controles básicos
                DensityControl(
                    density = config.density,
                    onDensityChanged = { newDensity ->
                        onConfigChanged(config.copy(density = newDensity))
                    }
                )
                
                SpeedControl(
                    speed = config.speed,
                    onSpeedChanged = { newSpeed ->
                        onConfigChanged(config.copy(speed = newSpeed))
                    }
                )
                
                BrightnessControl(
                    brightness = config.brightness,
                    onBrightnessChanged = { newBrightness ->
                        onConfigChanged(config.copy(brightness = newBrightness))
                    }
                )
                
                // Controles avançados (condicional)
                if (showAdvanced) {
                    AdvancedControls(
                        config = config,
                        onConfigChanged = onConfigChanged
                    )
                }
                
                // Botões de ação
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onReset,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Green.copy(alpha = 0.3f),
                            contentColor = Color.Green
                        )
                    ) {
                        Text("Redefinir")
                    }
                }
            }
        }
    }
    
    /**
     * Controle de densidade de partículas.
     */
    @Composable
    private fun DensityControl(
        density: Float,
        onDensityChanged: (Float) -> Unit
    ) {
        ControlSlider(
            label = "Densidade",
            value = density,
            onValueChange = onDensityChanged,
            valueRange = 0.5f..3.0f,
            steps = 10,
            icon = Icons.Default.DensityMedium
        )
    }
    
    /**
     * Controle de velocidade das partículas.
     */
    @Composable
    private fun SpeedControl(
        speed: Float,
        onSpeedChanged: (Float) -> Unit
    ) {
        ControlSlider(
            label = "Velocidade",
            value = speed,
            onValueChange = onSpeedChanged,
            valueRange = 0.1f..5.0f,
            steps = 20,
            icon = Icons.Default.Speed
        )
    }
    
    /**
     * Controle de brilho dos glifos.
     */
    @Composable
    private fun BrightnessControl(
        brightness: Float,
        onBrightnessChanged: (Float) -> Unit
    ) {
        ControlSlider(
            label = "Brilho",
            value = brightness,
            onValueChange = onBrightnessChanged,
            valueRange = 0.1f..2.0f,
            steps = 10,
            icon = Icons.Default.BrightnessMedium
        )
    }
    
    /**
     * Controles avançados.
     */
    @Composable
    private fun AdvancedControls(
        config: MatrixConfig,
        onConfigChanged: (MatrixConfig) -> Unit
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Green.copy(alpha = 0.1f))
                .padding(12.dp)
        ) {
            Text(
                text = "Controles Avançados",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Green
            )
            
            // Controle de comprimento do rastro
            ControlSlider(
                label = "Comprimento do Rastro",
                value = config.trailLength.toFloat(),
                onValueChange = { newValue ->
                    onConfigChanged(config.copy(trailLength = newValue.toInt()))
                },
                valueRange = 1f..30f,
                steps = 29,
                icon = Icons.Default.Timeline
            )
            
            // Controle de chance de destaque
            ControlSlider(
                label = "Chance de Destaque",
                value = config.highlightChance,
                onValueChange = { newValue ->
                    onConfigChanged(config.copy(highlightChance = newValue))
                },
                valueRange = 0f..0.2f,
                steps = 20,
                icon = Icons.Default.Star
            )
        }
    }
    
    /**
     * Slider genérico para controle de parâmetros.
     */
    @Composable
    private fun ControlSlider(
        label: String,
        value: Float,
        onValueChange: (Float) -> Unit,
        valueRange: ClosedFloatingPointRange<Float>,
        steps: Int,
        icon: androidx.compose.ui.graphics.vector.ImageVector
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = Color.Green,
                        modifier = Modifier.size(20.dp)
                    )
                    
                    Text(
                        text = label,
                        fontSize = 14.sp,
                        color = Color.Green
                    )
                }
                
                Text(
                    text = "%.2f".format(value),
                    fontSize = 14.sp,
                    color = Color.Green.copy(alpha = 0.8f)
                )
            }
            
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps,
                colors = SliderDefaults.colors(
                    thumbColor = Color.Green,
                    activeTrackColor = Color.Green,
                    inactiveTrackColor = Color.Green.copy(alpha = 0.3f)
                )
            )
        }
    }
    
    /**
     * Composable que combina efeito Matrix com controles interativos.
     */
    @Composable
    fun InteractiveMatrixEffect(
        modifier: Modifier = Modifier,
        initialConfig: MatrixConfig = MatrixConfig.Default,
        showControls: Boolean = true
    ) {
        var config by remember { mutableStateOf(initialConfig) }
        var controlsVisible by remember { mutableStateOf(showControls) }
        
        Box(modifier = modifier.fillMaxSize()) {
            // Efeito Matrix
            MatrixEffect(
                modifier = Modifier.fillMaxSize(),
                config = config,
                enabled = true
            )
            
            // Controles flutuantes
            if (controlsVisible) {
                MatrixControlPanel(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(0.9f),
                    config = config,
                    onConfigChanged = { newConfig -> config = newConfig },
                    onToggleVisibility = { controlsVisible = !controlsVisible },
                    onReset = { config = MatrixConfig.Default }
                )
            } else {
                // Botão flutuante para mostrar controles
                FloatingActionButton(
                    onClick = { controlsVisible = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    containerColor = Color.Green.copy(alpha = 0.8f),
                    contentColor = Color.Black
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Mostrar controles"
                    )
                }
            }
        }
    }
}