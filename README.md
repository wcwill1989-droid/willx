# WillX Chat (OpenRouter)

Este projeto foi estabilizado para compilar e gerar APK, e agora contém um app de chat funcional que usa a API do **OpenRouter**.

## Como rodar

- Gere o APK:

```bash
./gradlew clean assembleDebug
```

- APK gerado:

`app/build/outputs/apk/debug/app-debug.apk`

## Configuração (por usuário, dentro do app)

O app **não** depende mais de `OPENROUTER_API_KEY` no Gradle.

1. Abra o app
2. Toque em **Config**
3. Preencha:
   - **OpenRouter API Key**
   - **Modelo**
     - Pode ser `AUTO` (prioriza grátis → barato)
     - Ou um modelo explícito, ex: `openai/gpt-4o-mini`
4. (Opcional) Ative **Web search**
5. Toque em **Salvar**

## Funcionalidades atuais

## 1) Chat com OpenRouter

- Endpoint: `https://openrouter.ai/api/v1/chat/completions`
- Cliente HTTP: OkHttp
- Request: formato compatível com OpenAI Chat Completions

Arquivos relevantes:
- `app/src/minimal/kotlin/com/willx/ai/OpenRouterClient.kt`
- `app/src/minimal/kotlin/com/willx/ai/ChatActivity.kt`

### Streaming de resposta (UI)

O app usa `stream=true` no OpenRouter e consome `text/event-stream` (SSE). A resposta do assistant aparece **incrementalmente** na UI conforme os chunks chegam.

Enquanto está streamando, aparece o botão **Parar** para cancelar e manter o texto parcial.

## 2) Histórico persistente

O app salva o histórico localmente como JSON e recarrega ao abrir.

- Arquivo: `filesDir/chat_history.json`
- Implementação: `ChatHistoryStore.kt`

## 3) Suporte a arquivos (texto)

Você pode anexar um arquivo de texto (SAF) pelo botão **Arquivo**.

- Tipos: `text/*`
- O conteúdo do arquivo é injetado no prompt enviado.

Arquivos:
- `ContentResolverUtils.kt`
- Integração em `ChatActivity.kt` e `OpenRouterClient.kt`

## 4) Pesquisa web em tempo real (sem chave)

Quando habilitada em Configurações, o app faz uma busca rápida no Wikipedia OpenSearch antes de enviar e injeta links no prompt:

- API: `https://en.wikipedia.org/w/api.php?action=opensearch&...`
- Implementação: `WikipediaSearch.kt`

## 4.1) Pesquisa web profunda (Jina)

O app também suporta pesquisa web via **Jina** (requer API key da Jina). Existem 2 modos:

- **JINA_SEARCH**: usa `https://s.jina.ai/` e retorna SERP + snippets.
- **JINA_DEEPSEARCH**: usa `https://deepsearch.jina.ai/v1/chat/completions` para investigação mais profunda.

### Streaming (SSE)

O modo `JINA_DEEPSEARCH` é chamado com `stream=true` e `Accept: text/event-stream` (SSE). O app acumula os chunks `data:` (campo `choices[0].delta.content`) até receber `[DONE]`.

Isso reduz a chance de timeout em requisições longas.

### Integração na UI (etapa de pesquisa)

Quando o **Web provider** está em `JINA_DEEPSEARCH`, o app executa uma etapa de **pesquisa em streaming** e mostra um bubble `research` atualizando ao vivo. Ao terminar, o resultado é anexado como contexto e só então o OpenRouter é chamado.

O botão **Parar** cancela a etapa atual (busca ou geração).

Como configurar:

1. Abra **Config**
2. Ative **Web search**
3. Em **Web provider**, use:
   - `JINA_SEARCH` ou `JINA_DEEPSEARCH`
4. Cole sua **Jina API Key**
5. Salve

Arquivos:

- `JinaSearch.kt`
- `JinaDeepSearch.kt`
- `AppSettings.kt` (keys + provider)

## 5) Modelos: prioridade grátis → barato

No campo de modelo você pode usar:

- `AUTO`: escolhe o primeiro da lista curada.

Lista curada (ordem):
- `meta-llama/llama-3.1-8b-instruct:free`
- `google/gemma-2-9b-it:free`
- `mistralai/mistral-7b-instruct:free`
- `openai/gpt-4o-mini`

Implementação:
- `ModelCatalog.kt`

## Observações de arquitetura

- UI: Jetpack Compose (Material 3)
- Persistência simples: JSON em `filesDir` + DataStore Preferences
- Configurações: DataStore (`AppSettings.kt`)
- Para manter o build estável, o app usa um sourceSet mínimo:
  - `src/minimal` (manifest + código)
  - `src/main/res` e `src/main/assets` são reutilizados

## Roadmap (próximas implementações)

- Modelos dinâmicos:
  - Consumir `/models` do OpenRouter e ordenar por custo/"free".
  - UI com dropdown + detalhes (preço por token, contexto, etc.).

- Histórico de conversas (múltiplas conversas):
  - Em vez de um único `chat_history.json`, criar uma lista de conversas.
  - Nomear, duplicar, exportar.

- Suporte avançado de arquivos:
  - PDF/Docx (extração de texto)
  - Imagens (OCR)
  - Upload como "reference" (dependendo do modelo)

- Web search mais completo:
  - Múltiplas fontes (DuckDuckGo/Brave) via backend/proxy
  - Cache + deduplicação
  - Resumos com citações
  - DeepSearch em streaming (SSE)

- Streaming de respostas:
  - Usar `stream=true` (se disponível no OpenRouter/modelo) e atualizar UI incrementalmente.

- Segurança:
  - Opção de armazenar API key com criptografia (Android Keystore)

- Teclado (IME):
  - Reintroduzir um IME que injeta texto da resposta no campo atual.

