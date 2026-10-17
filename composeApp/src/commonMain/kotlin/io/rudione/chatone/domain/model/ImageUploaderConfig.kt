package io.rudione.chatone.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class UploaderFormField(
    val name: String = "",
    val value: String = ""
)

@Serializable
data class ImageUploaderConfig(
    val enabled: Boolean = false,
    val askOnUpload: Boolean = false,
    val requestUrl: String = DEFAULT_REQUEST_URL,
    val formField: String = DEFAULT_FORM_FIELD,
    val extraHeaders: String = DEFAULT_EXTRA_HEADERS,
    val linkFormat: String = DEFAULT_LINK_FORMAT,
    val deletionLinkFormat: String = "",
    val formFields: List<UploaderFormField> = emptyList()
) {
    val isUsable: Boolean get() = enabled && requestUrl.isNotBlank() && formField.isNotBlank()

    val extraFormFields: List<UploaderFormField>
        get() = formFields
            .map { it.copy(name = it.name.trim()) }
            .filter { it.name.isNotEmpty() && !it.name.equals(formField.trim(), ignoreCase = true) }
            .distinctBy { it.name }

    fun sealed(seal: (String) -> String): ImageUploaderConfig = copy(
        extraHeaders = seal(extraHeaders),
        formFields = formFields.map { it.copy(value = seal(it.value)) }
    )

    fun opened(open: (String) -> String): ImageUploaderConfig = copy(
        extraHeaders = open(extraHeaders),
        formFields = formFields.map { it.copy(value = open(it.value)) }
    )

    fun withStandardService(): ImageUploaderConfig {
        if (requestUrl.isNotBlank()) return this
        return copy(
            requestUrl = DEFAULT_REQUEST_URL,
            formField = formField.ifBlank { DEFAULT_FORM_FIELD },
            extraHeaders = extraHeaders.ifBlank { DEFAULT_EXTRA_HEADERS },
            linkFormat = linkFormat.ifBlank { DEFAULT_LINK_FORMAT }
        )
    }

    companion object {
        const val DEFAULT_REQUEST_URL = "https://p.ftk.li/upload"
        const val DEFAULT_FORM_FIELD = "image"
        const val DEFAULT_EXTRA_HEADERS = "POST"
        const val DEFAULT_LINK_FORMAT = "{url}"
    }
}
