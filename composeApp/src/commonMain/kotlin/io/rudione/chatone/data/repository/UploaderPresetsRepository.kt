package io.rudione.chatone.data.repository

import io.rudione.chatone.data.local.ChatoneDatabase
import io.rudione.chatone.domain.model.ImageUploaderConfig
import io.rudione.chatone.domain.model.UploaderPreset
import io.rudione.chatone.util.security.SecretVault
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

class UploaderPresetsRepository(private val database: ChatoneDatabase) {
    private val queries = database.laboratoryQueries
    private val json = Json { ignoreUnknownKeys = true }
    private val _presets = MutableStateFlow(load())
    val presets = _presets.asStateFlow()

    fun save(name: String, config: ImageUploaderConfig) {
        val cleanName = name.trim().take(60)
        if (cleanName.isEmpty() || config.requestUrl.isBlank() || config.formField.isBlank()) return
        val existing = _presets.value.firstOrNull { it.name == cleanName }
        if (existing == null && _presets.value.size >= MAX_PRESETS) return
        val preset = UploaderPreset(existing?.id ?: Random.nextLong().toString(16), cleanName, config)
        queries.saveUploaderPreset(preset.id, preset.name, SecretVault.seal(json.encodeToString(preset)))
        _presets.value = load()
    }

    fun remove(id: String) {
        queries.removeUploaderPreset(id)
        _presets.value = load()
    }

    private fun load(): List<UploaderPreset> = queries.selectUploaderPresets().executeAsList().mapNotNull { row ->
        runCatching { json.decodeFromString<UploaderPreset>(SecretVault.open(row.payload)) }.getOrNull()
    }.take(MAX_PRESETS)

    companion object { const val MAX_PRESETS = 8 }
}
