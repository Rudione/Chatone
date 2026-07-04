package io.rudione.chatone.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ImageUploaderConfig(
    val enabled: Boolean = false,
    val askOnUpload: Boolean = false,
    val requestUrl: String = "",
    val formField: String = "image",
    val extraHeaders: String = "",
    val linkFormat: String = "{url}",
    val deletionLinkFormat: String = ""
) {
    val isUsable: Boolean get() = enabled && requestUrl.isNotBlank() && formField.isNotBlank()
}
