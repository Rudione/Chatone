package io.rudione.chatone.domain.model

data class LoginCredentials(
    val username: String,
    val userId: String,
    val clientId: String,
    val oauthToken: String,
    val state: String
) {
    val isComplete: Boolean
        get() = oauthToken.isNotBlank()

    override fun toString(): String = "LoginCredentials(username=$username, userId=$userId)"
}
