package com.willx.ai

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

object AppSettings {
    private val KEY_OPENROUTER_API_KEY = stringPreferencesKey("openrouter_api_key")
    private val KEY_MODEL = stringPreferencesKey("openrouter_model")
    private val KEY_WEB_SEARCH_ENABLED = booleanPreferencesKey("web_search_enabled")
    private val KEY_JINA_API_KEY = stringPreferencesKey("jina_api_key")
    private val KEY_WEB_PROVIDER = stringPreferencesKey("web_provider")
    private val KEY_THEME = stringPreferencesKey("theme")

    fun apiKeyFlow(context: Context): Flow<String> =
        context.dataStore.data.map { it[KEY_OPENROUTER_API_KEY].orEmpty() }

    fun modelFlow(context: Context): Flow<String> =
        context.dataStore.data.map { it[KEY_MODEL] ?: BuildConfig.OPENROUTER_DEFAULT_MODEL }

    fun webSearchEnabledFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[KEY_WEB_SEARCH_ENABLED] ?: false }

    fun jinaApiKeyFlow(context: Context): Flow<String> =
        context.dataStore.data.map { it[KEY_JINA_API_KEY].orEmpty() }

    fun webProviderFlow(context: Context): Flow<String> =
        context.dataStore.data.map { it[KEY_WEB_PROVIDER] ?: WebProvider.WIKIPEDIA }

    fun themeFlow(context: Context): Flow<String> =
        context.dataStore.data.map { it[KEY_THEME] ?: ThemeMode.DEFAULT }

    suspend fun setApiKey(context: Context, value: String) {
        context.dataStore.edit { it[KEY_OPENROUTER_API_KEY] = value }
    }

    suspend fun setModel(context: Context, value: String) {
        context.dataStore.edit { it[KEY_MODEL] = value }
    }

    suspend fun setWebSearchEnabled(context: Context, value: Boolean) {
        context.dataStore.edit { it[KEY_WEB_SEARCH_ENABLED] = value }
    }

    suspend fun setJinaApiKey(context: Context, value: String) {
        context.dataStore.edit { it[KEY_JINA_API_KEY] = value }
    }

    suspend fun setWebProvider(context: Context, value: String) {
        context.dataStore.edit { it[KEY_WEB_PROVIDER] = value }
    }

    suspend fun setTheme(context: Context, value: String) {
        context.dataStore.edit { it[KEY_THEME] = value }
    }
}

object WebProvider {
    const val WIKIPEDIA = "WIKIPEDIA"
    const val JINA_SEARCH = "JINA_SEARCH"
    const val JINA_DEEPSEARCH = "JINA_DEEPSEARCH"
}

object ThemeMode {
    const val DEFAULT = "DEFAULT"
    const val DARK = "DARK"
    const val MATRIX = "MATRIX"
}
