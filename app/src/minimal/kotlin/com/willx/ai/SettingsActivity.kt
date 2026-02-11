package com.willx.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Save
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val activity = this@SettingsActivity
            val themeMode by AppSettings.themeFlow(activity).collectAsStateWithLifecycle(initialValue = ThemeMode.DEFAULT)
            WillXTheme(mode = themeMode) {
                val apiKey by AppSettings.apiKeyFlow(activity).collectAsStateWithLifecycle(initialValue = "")
                val model by AppSettings.modelFlow(activity).collectAsStateWithLifecycle(initialValue = BuildConfig.OPENROUTER_DEFAULT_MODEL)
                val webSearchEnabled by AppSettings.webSearchEnabledFlow(activity).collectAsStateWithLifecycle(initialValue = false)
                val jinaApiKey by AppSettings.jinaApiKeyFlow(activity).collectAsStateWithLifecycle(initialValue = "")
                val webProvider by AppSettings.webProviderFlow(activity).collectAsStateWithLifecycle(initialValue = WebProvider.WIKIPEDIA)
                val theme by AppSettings.themeFlow(activity).collectAsStateWithLifecycle(initialValue = ThemeMode.DEFAULT)

                 var apiKeyDraft by remember(apiKey) { mutableStateOf(apiKey) }
                 var modelDraft by remember(model) { mutableStateOf(model) }
                 var status by remember { mutableStateOf("") }
                 var webSearchDraft by remember(webSearchEnabled) { mutableStateOf(webSearchEnabled) }
                 var jinaApiKeyDraft by remember(jinaApiKey) { mutableStateOf(jinaApiKey) }
                 var webProviderDraft by remember(webProvider) { mutableStateOf(webProvider) }
                 var themeDraft by remember(theme) { mutableStateOf(theme) }
                 var isSaving by remember { mutableStateOf(false) }
                 var isSaved by remember { mutableStateOf(false) }
                 val snackbarHostState = remember { SnackbarHostState() }
                 val scope = rememberCoroutineScope()

                 fun save() {
                     isSaving = true
                     scope.launch(Dispatchers.IO) {
                         AppSettings.setApiKey(this@SettingsActivity, apiKeyDraft.trim())
                         AppSettings.setModel(
                             this@SettingsActivity,
                             modelDraft.trim().ifBlank { ModelCatalog.AUTO }
                         )
                         AppSettings.setWebSearchEnabled(this@SettingsActivity, webSearchDraft)
                         AppSettings.setJinaApiKey(this@SettingsActivity, jinaApiKeyDraft.trim())
                         AppSettings.setWebProvider(this@SettingsActivity, webProviderDraft)
                         AppSettings.setTheme(this@SettingsActivity, themeDraft)
                         
                         // Mostrar Snackbar de confirmação
                         scope.launch {
                             snackbarHostState.showSnackbar(
                                 message = "✅ Configurações salvas com sucesso!",
                                 actionLabel = "OK",
                                 duration = androidx.compose.material3.SnackbarDuration.Short
                             )
                         }
                         
                         // Atualizar estados visuais
                         withContext(Dispatchers.Main) {
                             isSaving = false
                             isSaved = true
                             status = "Salvo"
                             
                             // Resetar o estado de "salvo" após 3 segundos
                             scope.launch {
                                 kotlinx.coroutines.delay(3000)
                                 isSaved = false
                             }
                         }
                     }
                 }

                // Salvar automaticamente quando a atividade está sendo destruída
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.withContext(Dispatchers.Main.immediate) {
                        // Esta função será chamada quando o efeito for desmontado
                        // (quando a atividade estiver sendo destruída)
                        // Não vamos salvar automaticamente aqui para não sobrecarregar
                        // O usuário deve clicar no botão Salvar explicitamente
                    }
                }

                // Função para verificar se há mudanças não salvas
                fun hasUnsavedChanges(): Boolean {
                    return apiKeyDraft != apiKey ||
                            modelDraft != model ||
                            webSearchDraft != webSearchEnabled ||
                            jinaApiKeyDraft != jinaApiKey ||
                            webProviderDraft != webProvider ||
                            themeDraft != theme
                }

                Scaffold(
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    topBar = { TopAppBar(title = { Text("Configurações") }) }
                ) { padding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            val hasChanges = hasUnsavedChanges()
                            
                            if (hasChanges) {
                                Text(
                                    text = "⚠️ Há mudanças não salvas",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }

                            // Seção: Credenciais API
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        "🔑 Credenciais API",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    
                                    OutlinedTextField(
                                        modifier = Modifier.fillMaxWidth(),
                                        value = apiKeyDraft,
                                        onValueChange = { apiKeyDraft = it },
                                        label = { Text("OpenRouter API Key") },
                                        visualTransformation = PasswordVisualTransformation(),
                                        singleLine = true,
                                    )
                                    
                                    OutlinedTextField(
                                        modifier = Modifier.fillMaxWidth(),
                                        value = jinaApiKeyDraft,
                                        onValueChange = { jinaApiKeyDraft = it },
                                        label = { Text("Jina API Key (opcional)") },
                                        visualTransformation = PasswordVisualTransformation(),
                                        singleLine = true,
                                    )
                                }
                            }
                            
                            // Seção: Configurações do Modelo
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        "🤖 Modelo de IA",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    
                                    OutlinedTextField(
                                        modifier = Modifier.fillMaxWidth(),
                                        value = modelDraft,
                                        onValueChange = { modelDraft = it },
                                        label = { Text("Modelo (ex: openai/gpt-4o-mini)") },
                                        singleLine = true,
                                    )
                                    
                                    Text(
                                        text = "Sugestões:\n" +
                                            (listOf(ModelCatalog.AUTO) + ModelCatalog.preferredOrder).joinToString("\n"),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            
                            // Seção: Web Search
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        "🌐 Busca na Web",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Habilitar busca na web")
                                        Switch(
                                            checked = webSearchDraft,
                                            onCheckedChange = { webSearchDraft = it }
                                        )
                                    }
                                    
                                    OutlinedTextField(
                                        modifier = Modifier.fillMaxWidth(),
                                        value = webProviderDraft,
                                        onValueChange = { webProviderDraft = it },
                                        label = { Text("Provedor de busca") },
                                        singleLine = true,
                                    )
                                }
                            }
                            
                            // Seção: Aparência
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        "🎨 Aparência",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    
                                    OutlinedTextField(
                                        modifier = Modifier.fillMaxWidth(),
                                        value = themeDraft,
                                        onValueChange = { themeDraft = it },
                                        label = { Text("Tema") },
                                        singleLine = true,
                                    )
                                }
                            }
                            
                            // Botão Salvar
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Button(
                                        onClick = { save() },
                                        enabled = !isSaving,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = when {
                                                isSaved -> MaterialTheme.colorScheme.tertiary
                                                isSaving -> MaterialTheme.colorScheme.secondary
                                                else -> MaterialTheme.colorScheme.primary
                                            },
                                            contentColor = when {
                                                isSaved -> MaterialTheme.colorScheme.onTertiary
                                                isSaving -> MaterialTheme.colorScheme.onSecondary
                                                else -> MaterialTheme.colorScheme.onPrimary
                                            }
                                        )
                                    ) {
                                        if (isSaving) {
                                            CircularProgressIndicator(
                                                color = MaterialTheme.colorScheme.onSecondary,
                                                strokeWidth = 2.dp,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        } else if (isSaved) {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = "Salvo",
                                                modifier = Modifier.size(24.dp)
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Filled.Save,
                                                contentDescription = "Salvar",
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        Text(
                                            text = when {
                                                isSaving -> "⏳ SALVANDO..."
                                                isSaved -> "✅ SALVO COM SUCESSO!"
                                                hasChanges -> "💾 SALVAR MUDANÇAS"
                                                else -> "💾 SALVAR CONFIGURAÇÕES"
                                            },
                                            style = MaterialTheme.typography.titleMedium,
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }
                                }
                            }

                            if (status.isNotBlank()) {
                                Text(
                                    text = status,
                                    color = if (status == "Salvo") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
