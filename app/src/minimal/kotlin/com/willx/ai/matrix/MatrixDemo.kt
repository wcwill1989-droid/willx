package com.willx.ai.matrix

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Demonstração completa do efeito Matrix com todas as funcionalidades.
 * Mostra diferentes variações e configurações do efeito.
 */
@Composable
fun MatrixDemoScreen() {
    var selectedDemo by remember { mutableStateOf(0) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Cabeçalho
        Text(
            text = "Demonstração do Efeito Matrix",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Green
        )
        
        Text(
            text = "Selecione uma demonstração para visualizar diferentes configurações do efeito Matrix.",
            fontSize = 14.sp,
            color = Color.Green.copy(alpha = 0.8f)
        )
        
        // Seleção de demonstração
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "Básico" to "Efeito padrão do Matrix",
                "Interativo" to "Controles em tempo real",
                "Responsivo" to "Adapta ao dispositivo",
                "Otimizado" to "Performance adaptativa",
                "Avancado" to "Efeitos visuais extras"
            ).forEachIndexed { index, (title, description) ->
                DemoCard(
                    title = title,
                    description = description,
                    isSelected = selectedDemo == index,
                    onClick = { selectedDemo = index },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        // Área de demonstração
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when (selectedDemo) {
                0 -> DemoBasic()
                1 -> DemoInteractive()
                2 -> DemoResponsive()
                3 -> DemoOptimized()
                4 -> DemoAdvanced()
            }
        }
        
        // Rodapé com informações
        Text(
            text = "Implementação completa do efeito 'chuva digital' dos filmes Matrix usando Jetpack Compose Canvas.",
            fontSize = 12.sp,
            color = Color.Green.copy(alpha = 0.6f),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Demonstração do efeito básico.
 */
@Composable
private fun DemoBasic() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Efeito Básico Matrix",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Green
        )
        
        Text(
            text = "Implementação clássica com glifos verdes caindo em cascata.",
            fontSize = 14.sp,
            color = Color.Green.copy(alpha = 0.8f)
        )
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            MatrixEffect(
                modifier = Modifier.fillMaxSize(),
                config = MatrixConfig.Default
            )
        }
    }
}

/**
 * Demonstração com controles interativos.
 */
@Composable
private fun DemoInteractive() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Efeito Interativo Matrix",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Green
        )
        
        Text(
            text = "Ajuste densidade, velocidade e brilho em tempo real.",
            fontSize = 14.sp,
            color = Color.Green.copy(alpha = 0.8f)
        )
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            MatrixControls.InteractiveMatrixEffect(
                modifier = Modifier.fillMaxSize(),
                showControls = true
            )
        }
    }
}

/**
 * Demonstração com responsividade.
 */
@Composable
private fun DemoResponsive() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Efeito Responsivo Matrix",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Green
        )
        
        Text(
            text = "Adapta-se automaticamente ao tamanho da tela e orientação.",
            fontSize = 14.sp,
            color = Color.Green.copy(alpha = 0.8f)
        )
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            ResponsiveMatrixEffect(
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Demonstração com otimização de performance.
 */
@Composable
private fun DemoOptimized() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Efeito Otimizado Matrix",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Green
        )
        
        Text(
            text = "Ajusta parâmetros automaticamente para manter performance suave.",
            fontSize = 14.sp,
            color = Color.Green.copy(alpha = 0.8f)
        )
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            MatrixPerformance.AdaptiveMatrixEffect(
                modifier = Modifier.fillMaxSize(),
                showMetrics = true
            )
        }
    }
}

/**
 * Demonstração com efeitos avançados.
 */
@Composable
private fun DemoAdvanced() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Efeito Avançado Matrix",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Green
        )
        
        Text(
            text = "Inclui rastros suaves, conexões entre partículas e efeitos de glitch.",
            fontSize = 14.sp,
            color = Color.Green.copy(alpha = 0.8f)
        )
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            MatrixEffect(
                modifier = Modifier.fillMaxSize(),
                config = MatrixConfig(
                    density = 1.5f,
                    speed = 1.2f,
                    brightness = 1.3f,
                    highlightChance = 0.08f,
                    trailLength = 20,
                    colorPrimary = Color(0xFF00FF7A),
                    colorSecondary = Color(0xFF00C853),
                    colorHighlight = Color(0xFFB9FFD9)
                )
            )
        }
    }
}

/**
 * Cartão de seleção de demonstração.
 */
@Composable
private fun DemoCard(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color.Green.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.5f),
            contentColor = Color.Green
        ),
        border = if (isSelected) CardDefaults.outlinedCardBorder() else null
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = description,
                fontSize = 11.sp,
                color = Color.Green.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * Função principal para testar o efeito Matrix.
 * Pode ser usada como tela independente ou como fundo de chat.
 */
@Composable
fun MatrixDemoPreview() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
    ) {
        MatrixDemoScreen()
    }
}