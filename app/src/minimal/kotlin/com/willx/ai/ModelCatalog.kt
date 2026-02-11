package com.willx.ai

object ModelCatalog {
    const val AUTO = "AUTO"

    /**
     * Lista curada (ordem importa): tenta grátis primeiro, depois modelos geralmente baratos.
     * Observação: os nomes podem mudar no OpenRouter; o usuário pode sempre digitar manualmente.
     */
    val preferredOrder: List<String> = listOf(
        "meta-llama/llama-3.1-8b-instruct:free",
        "google/gemma-2-9b-it:free",
        "mistralai/mistral-7b-instruct:free",
        "openai/gpt-4o-mini",
    )

    fun resolve(modelSetting: String): String {
        if (modelSetting.isBlank() || modelSetting == AUTO) {
            return preferredOrder.firstOrNull() ?: BuildConfig.OPENROUTER_DEFAULT_MODEL
        }
        return modelSetting
    }
}
