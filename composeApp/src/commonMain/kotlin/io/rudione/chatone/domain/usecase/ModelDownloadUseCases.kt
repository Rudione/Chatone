package io.rudione.chatone.domain.usecase

import io.rudione.chatone.data.remote.AiProviders
import io.rudione.chatone.data.remote.OllamaClient
import io.rudione.chatone.data.remote.OllamaInstalledModel
import io.rudione.chatone.data.repository.ModelDownloadRepository
import io.rudione.chatone.domain.model.ModelDownload
import kotlinx.coroutines.flow.StateFlow

class ObserveModelDownloadsUseCase(private val repository: ModelDownloadRepository) {
    operator fun invoke(): StateFlow<Map<String, ModelDownload>> = repository.downloads
}

class DownloadModelUseCase(private val repository: ModelDownloadRepository) {
    operator fun invoke(model: String) = repository.start(model)
}

class CancelModelDownloadUseCase(private val repository: ModelDownloadRepository) {
    operator fun invoke(model: String) = repository.cancel(model)
}

class RetryModelDownloadUseCase(private val repository: ModelDownloadRepository) {
    operator fun invoke(model: String) = repository.retry(model)
}

class ListInstalledModelsUseCase(private val ollama: OllamaClient) {
    suspend operator fun invoke(baseUrl: String = AiProviders.LOCAL_BASE_URL): List<OllamaInstalledModel> =
        ollama.installedModels(baseUrl)
}

class DeleteModelUseCase(private val ollama: OllamaClient) {
    suspend operator fun invoke(name: String, baseUrl: String = AiProviders.LOCAL_BASE_URL): Boolean =
        ollama.deleteModel(baseUrl, name)
}
