package com.willx.ai

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Call

class ChatActivity : ComponentActivity() {

    private enum class Stage {
        NONE,
        DEEPSEARCH,
        OPENROUTER,
    }

    private data class ChatItem(
        val id: Long,
        val role: String,
        val content: String,
        val pending: Boolean = false,
    )

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Remover barra de status (status bar)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE

        setContent {
            val themeMode by AppSettings.themeFlow(this).collectAsStateWithLifecycle(initialValue = ThemeMode.DEFAULT)
            val modelSetting by AppSettings.modelFlow(this).collectAsStateWithLifecycle(initialValue = BuildConfig.OPENROUTER_DEFAULT_MODEL)
            val webSearchEnabled by AppSettings.webSearchEnabledFlow(this).collectAsStateWithLifecycle(initialValue = false)
            val webProvider by AppSettings.webProviderFlow(this).collectAsStateWithLifecycle(initialValue = WebProvider.WIKIPEDIA)
            val jinaApiKey by AppSettings.jinaApiKeyFlow(this).collectAsStateWithLifecycle(initialValue = "")
            WillXTheme(mode = themeMode) {
                val messages = remember { mutableStateListOf<ChatItem>() }
                var input by remember { mutableStateOf("") }
                var status by remember { mutableStateOf("") }
                var attachmentUri by remember { mutableStateOf<Uri?>(null) }
                var attachmentText by remember { mutableStateOf<String?>(null) }
                var activeCall by remember { mutableStateOf<Call?>(null) }
                var isStreaming by remember { mutableStateOf(false) }
                var stage by remember { mutableStateOf(Stage.NONE) }
                val scope = rememberCoroutineScope()
                val listState = rememberLazyListState()
                val haptics = LocalHapticFeedback.current
                val chipScroll = rememberScrollState()
                val snackbarHostState = remember { SnackbarHostState() }

                val latestMessagesState by rememberUpdatedState(newValue = messages)
                var isLoading by remember { mutableStateOf(true) }

                // Collect messages from database Flow
                val dbMessagesFlow = remember { ChatHistoryStore.getMessagesFlow(this@ChatActivity) }
                val dbMessages by dbMessagesFlow.collectAsStateWithLifecycle(initialValue = emptyList())

                LaunchedEffect(dbMessages) {
                    if (isLoading) {
                        // First load: populate messages from database
                        messages.clear()
                        dbMessages.forEach {
                            messages.add(
                                ChatItem(
                                    id = it.ts,
                                    role = it.role,
                                    content = it.content,
                                )
                            )
                        }
                        isLoading = false
                    } else {
                        // Subsequent updates: sync with database (except for pending messages)
                        // We need to preserve pending messages that aren't in database yet
                        val currentPendingIds = messages.filter { it.pending }.map { it.id }.toSet()
                        val dbMessageIds = dbMessages.map { it.ts }.toSet()
                        
                        // Remove messages that are in our list but not in database (except pending)
                        messages.removeAll { !it.pending && it.id !in dbMessageIds }
                        
                        // Add or update messages from database
                        dbMessages.forEach { dbMsg ->
                            val existingIdx = messages.indexOfFirst { it.id == dbMsg.ts }
                            if (existingIdx >= 0) {
                                // Update existing message
                                val existing = messages[existingIdx]
                                if (!existing.pending) { // Don't overwrite pending messages
                                    messages[existingIdx] = ChatItem(
                                        id = dbMsg.ts,
                                        role = dbMsg.role,
                                        content = dbMsg.content,
                                        pending = false
                                    )
                                }
                            } else {
                                // Add new message from database
                                messages.add(
                                    ChatItem(
                                        id = dbMsg.ts,
                                        role = dbMsg.role,
                                        content = dbMsg.content,
                                    )
                                )
                            }
                        }
                    }
                    
                    // Auto-scroll to bottom when messages update
                    if (messages.isNotEmpty()) {
                        runCatching { listState.animateScrollToItem(messages.size - 1) }
                    }
                }

                LaunchedEffect(messages.size) {
                    if (messages.isNotEmpty()) {
                        runCatching { listState.animateScrollToItem(messages.size - 1) }
                    }
                }

                suspend fun persist() {
                    val copy = messages.map {
                        ChatHistoryStore.Message(
                            role = it.role,
                            content = it.content,
                            ts = it.id, // Usar o ID como timestamp (já é timestamp)
                        )
                    }
                    ChatHistoryStore.save(this@ChatActivity, copy)
                }

                val filePicker = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocument(),
                    onResult = { uri ->
                        attachmentUri = uri
                        attachmentText = if (uri != null) {
                            runCatching { ContentResolverUtils.readText(contentResolver, uri) }.getOrNull()
                        } else {
                            null
                        }
                    }
                )

                fun send() {
                    val text = input.trim()
                    if (text.isEmpty()) return

                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    input = ""
                    status = "Enviando..."
                    val now = System.currentTimeMillis()
                    messages.add(ChatItem(id = now, role = "user", content = text))

                    val pendingId = now + 1
                    messages.add(ChatItem(id = pendingId, role = "assistant", content = "", pending = true))
                    scope.launch {
                        persist()
                    }

                    isStreaming = true
                    stage = Stage.OPENROUTER

                    fun startOpenRouter(extra: String?) {
                        stage = Stage.OPENROUTER
                        activeCall = OpenRouterClient.sendChatStreaming(
                            context = this@ChatActivity,
                            userMessage = text,
                            extraContext = extra,
                            onDelta = { chunk ->
                                val idx = messages.indexOfFirst { it.id == pendingId }
                                if (idx >= 0) {
                                    val cur = messages[idx]
                                    messages[idx] = cur.copy(content = cur.content + chunk)
                                }
                            },
                            onComplete = { res ->
                                isStreaming = false
                                stage = Stage.NONE
                                activeCall = null
                                res
                                    .onSuccess {
                                        val idx = messages.indexOfFirst { it.id == pendingId }
                                        if (idx >= 0) {
                                            val cur = messages[idx]
                                            messages[idx] = cur.copy(pending = false)
                                        }
                                        status = ""
                                        scope.launch {
                                            persist()
                                        }
                                    }
                                    .onFailure { e ->
                                        val errorMessage = "Erro: ${e.message ?: e.javaClass.simpleName}"
                                        status = errorMessage
                                        // Mostrar Snackbar com o erro
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                message = errorMessage,
                                                actionLabel = "OK",
                                                duration = androidx.compose.material3.SnackbarDuration.Long
                                            )
                                        }
                                        val idx = messages.indexOfFirst { it.id == pendingId }
                                        if (idx >= 0) {
                                            val cur = messages[idx]
                                            messages[idx] = cur.copy(content = if (cur.content.isBlank()) "(falhou)" else cur.content, pending = false)
                                        }
                                    }
                            },
                        )
                    }

                    if (webSearchEnabled && webProvider == WebProvider.JINA_DEEPSEARCH && jinaApiKey.trim().isNotBlank()) {
                        stage = Stage.DEEPSEARCH
                        val researchId = pendingId - 1
                        messages.add(ChatItem(id = researchId, role = "research", content = "", pending = true))

                        val researchSb = StringBuilder()
                        activeCall = JinaDeepSearch.deepSearchStreaming(
                            apiKey = jinaApiKey.trim(),
                            query = text,
                            onDelta = { chunk ->
                                researchSb.append(chunk)
                                val idx = messages.indexOfFirst { it.id == researchId }
                                if (idx >= 0) {
                                    val cur = messages[idx]
                                    messages[idx] = cur.copy(content = cur.content + chunk)
                                }
                            },
                            onComplete = { res ->
                                res
                                    .onSuccess {
                                        val idx = messages.indexOfFirst { it.id == researchId }
                                        if (idx >= 0) {
                                            val cur = messages[idx]
                                            messages[idx] = cur.copy(pending = false)
                                        }
                                        val extra = buildString {
                                            if (!attachmentText.isNullOrBlank()) {
                                                append(attachmentText)
                                            }
                                            if (researchSb.isNotBlank()) {
                                                if (isNotEmpty()) append("\n\n")
                                                append("[Web deep search: Jina DeepSearch]\n")
                                                append(researchSb.toString())
                                            }
                                        }.ifBlank { null }

                                        startOpenRouter(extra)
                                    }
                                    .onFailure { e ->
                                        val idx = messages.indexOfFirst { it.id == researchId }
                                        if (idx >= 0) {
                                            val cur = messages[idx]
                                            messages[idx] = cur.copy(content = if (cur.content.isBlank()) "(falhou)" else cur.content, pending = false)
                                        }
                                        // fallback: segue sem deepsearch
                                        startOpenRouter(attachmentText)
                                    }
                            },
                        )
                        return
                    }

                    startOpenRouter(attachmentText)
                }

                Scaffold(
                    containerColor = if (themeMode == ThemeMode.MATRIX) Color.Transparent else MaterialTheme.colorScheme.background,
                    contentWindowInsets = WindowInsets.ime,
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    topBar = {
                        TopAppBar(
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("WillX Chat")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    when (themeMode) {
                                        ThemeMode.MATRIX -> {
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        color = Color(0xFF00FF7A).copy(alpha = 0.2f),
                                                        shape = RoundedCornerShape(4.dp)
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    "MATRIX",
                                                    color = Color(0xFF00FF7A),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        ThemeMode.DARK -> {
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        color = Color.Gray.copy(alpha = 0.2f),
                                                        shape = RoundedCornerShape(4.dp)
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    "ESCURO",
                                                    color = Color.Gray,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        else -> {
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        color = Color.Blue.copy(alpha = 0.2f),
                                                        shape = RoundedCornerShape(4.dp)
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    "CLARO",
                                                    color = Color.Blue,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            },
                            actions = {
                                IconButton(onClick = {
                                    val next = when (themeMode) {
                                        ThemeMode.DEFAULT -> ThemeMode.DARK
                                        ThemeMode.DARK -> ThemeMode.MATRIX
                                        else -> ThemeMode.DEFAULT
                                    }
                                    scope.launch(Dispatchers.IO) {
                                        AppSettings.setTheme(this@ChatActivity, next)
                                    }
                                }) {
                                    Icon(imageVector = Icons.Filled.Brightness6, contentDescription = "Tema")
                                }
                                IconButton(onClick = {
                                    messages.clear()
                                    attachmentUri = null
                                    attachmentText = null
                                    scope.launch {
                                        ChatHistoryStore.clear(this@ChatActivity)
                                    }
                                }) {
                                    Icon(imageVector = Icons.Filled.DeleteSweep, contentDescription = "Limpar")
                                }
                                IconButton(onClick = {
                                    startActivity(Intent(this@ChatActivity, SettingsActivity::class.java))
                                }) {
                                    Icon(imageVector = Icons.Filled.Settings, contentDescription = "Config")
                                }
                            }
                        )
                    }
                ) { padding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(chipScroll),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val webLabel = if (webSearchEnabled) webProvider else "OFF"
                            AssistChip(
                                onClick = { startActivity(Intent(this@ChatActivity, SettingsActivity::class.java)) },
                                label = {
                                    Text(
                                        text = "Model: ${ModelCatalog.resolve(modelSetting)}",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                },
                            )
                            AssistChip(
                                onClick = { startActivity(Intent(this@ChatActivity, SettingsActivity::class.java)) },
                                label = {
                                    Text(
                                        text = "Web: $webLabel",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                },
                            )
                            AssistChip(
                                onClick = { filePicker.launch(arrayOf("text/*")) },
                                label = {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Filled.AttachFile, contentDescription = null)
                                        Text(
                                            text = if (attachmentUri == null) "OFF" else "ON",
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                    }
                                },
                            )
                            AssistChip(
                                onClick = {
                                    startActivity(Intent(this@ChatActivity, SettingsActivity::class.java))
                                },
                                label = {
                                    Text(
                                        text = "Tema: $themeMode",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                },
                            )
                        }

                        if (status.isNotBlank() || isStreaming) {
                            Row(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (isStreaming) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                                Text(
                                    text = if (status.isNotBlank()) status else when (stage) {
                                        Stage.DEEPSEARCH -> "Buscando na web..."
                                        Stage.OPENROUTER -> "Gerando resposta..."
                                        else -> "Processando..."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }

                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            state = listState,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(messages) { item ->
                                ChatBubble(
                                    role = item.role,
                                    text = item.content,
                                    pending = item.pending,
                                    matrix = themeMode == ThemeMode.MATRIX,
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    when (themeMode) {
                                        ThemeMode.MATRIX -> Color(0xFF001E0C).copy(alpha = 0.6f)
                                        ThemeMode.DARK -> MaterialTheme.colorScheme.surfaceVariant
                                        else -> MaterialTheme.colorScheme.surface
                                    },
                                    RoundedCornerShape(24.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Botão de anexar arquivo
                            IconButton(
                                onClick = { filePicker.launch(arrayOf("text/*")) },
                                modifier = Modifier
                                    .background(
                                        if (attachmentUri != null) {
                                            when (themeMode) {
                                                ThemeMode.MATRIX -> Color(0xFF00FF7A).copy(alpha = 0.2f)
                                                else -> MaterialTheme.colorScheme.primaryContainer
                                            }
                                        } else Color.Transparent,
                                        RoundedCornerShape(12.dp)
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AttachFile,
                                    contentDescription = "Anexar arquivo",
                                    tint = if (attachmentUri != null) {
                                        when (themeMode) {
                                            ThemeMode.MATRIX -> Color(0xFF00FF7A)
                                            else -> MaterialTheme.colorScheme.primary
                                        }
                                    } else {
                                        when (themeMode) {
                                            ThemeMode.MATRIX -> Color(0xFFB9FFD9).copy(alpha = 0.6f)
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    }
                                )
                            }

                            // Campo de texto
                            OutlinedTextField(
                                modifier = Modifier
                                    .weight(1f),
                                value = input,
                                onValueChange = { input = it },
                                placeholder = { 
                                    Text(
                                        "Digite sua mensagem...", 
                                        color = when (themeMode) {
                                            ThemeMode.MATRIX -> Color(0xFFB9FFD9).copy(alpha = 0.5f)
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        }
                                    ) 
                                },
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    color = when (themeMode) {
                                        ThemeMode.MATRIX -> Color(0xFFB9FFD9)
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                ),
                                singleLine = false,
                                maxLines = 4,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = when (themeMode) {
                                        ThemeMode.MATRIX -> Color(0xFF00FF7A)
                                        else -> MaterialTheme.colorScheme.primary
                                    },
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )

                            // Botão de parar/enviar
                            if (isStreaming) {
                                IconButton(
                                    onClick = {
                                        activeCall?.cancel()
                                        activeCall = null
                                        isStreaming = false
                                        stage = Stage.NONE
                                        status = "Cancelado"
                                    },
                                    modifier = Modifier
                                        .background(
                                            when (themeMode) {
                                                ThemeMode.MATRIX -> Color(0xFFFF0000).copy(alpha = 0.2f)
                                                else -> MaterialTheme.colorScheme.errorContainer
                                            },
                                            RoundedCornerShape(12.dp)
                                        )
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Stop,
                                        contentDescription = "Parar",
                                        tint = when (themeMode) {
                                            ThemeMode.MATRIX -> Color(0xFFFF4444)
                                            else -> MaterialTheme.colorScheme.error
                                        }
                                    )
                                }
                            } else {
                                IconButton(
                                    onClick = { send() },
                                    enabled = input.trim().isNotEmpty(),
                                    modifier = Modifier
                                        .background(
                                            if (input.trim().isNotEmpty()) {
                                                when (themeMode) {
                                                    ThemeMode.MATRIX -> Color(0xFF00FF7A).copy(alpha = 0.3f)
                                                    else -> MaterialTheme.colorScheme.primaryContainer
                                                }
                                            } else Color.Transparent,
                                            RoundedCornerShape(12.dp)
                                        )
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Send,
                                        contentDescription = "Enviar",
                                        tint = if (input.trim().isNotEmpty()) {
                                            when (themeMode) {
                                                ThemeMode.MATRIX -> Color(0xFF00FF7A)
                                                else -> MaterialTheme.colorScheme.primary
                                            }
                                        } else {
                                            when (themeMode) {
                                                ThemeMode.MATRIX -> Color(0xFFB9FFD9).copy(alpha = 0.3f)
                                                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun ChatBubble(
    role: String,
    text: String,
    pending: Boolean,
    matrix: Boolean,
) {
    val isUser = role == "user"
    val bg = when {
        matrix && isUser -> Color(0xFF0A2818)  // Verde escuro mais sólido para usuário
        matrix && !isUser -> Color(0xFF0D0D0D) // Preto quase puro para IA
        isUser -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val fg = when {
        matrix && isUser -> Color(0xFFE0FFE0)  // Verde claro quase branco para usuário
        matrix && !isUser -> Color(0xFF00FF41) // Verde Matrix brilhante para IA
        else -> MaterialTheme.colorScheme.onSurface
    }
    val glow = if (matrix) Color(0xFF00FF41) else Color.Transparent

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .shadow(
                    elevation = if (matrix) 8.dp else 2.dp,
                    shape = MaterialTheme.shapes.large,
                    ambientColor = glow,
                    spotColor = glow,
                )
        ) {
            Column(
                modifier = Modifier
                    .background(bg, MaterialTheme.shapes.large)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .widthIn(max = 320.dp)
            ) {
                Text(
                    text = if (isUser) "Você" else "AI",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = fg.copy(alpha = 0.9f),
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (pending) {
                    TypingDots(color = fg)
                } else {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = if (matrix) FontWeight.Medium else FontWeight.Normal
                        ),
                        color = fg,
                    )
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun TypingDots(color: Color) {
    val t = rememberInfiniteTransition(label = "dots")
    val p by t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
        label = "p"
    )

    val dots = when {
        p < 0.33f -> "."
        p < 0.66f -> ".."
        else -> "..."
    }

    Text(
        text = "digitando$dots",
        style = MaterialTheme.typography.bodyLarge,
        color = color.copy(alpha = 0.9f),
    )
}
