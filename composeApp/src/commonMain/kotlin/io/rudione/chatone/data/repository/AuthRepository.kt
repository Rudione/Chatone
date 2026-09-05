package io.rudione.chatone.data.repository

import io.github.aakira.napier.Napier
import io.rudione.chatone.data.local.ChatoneDatabase
import io.rudione.chatone.data.local.TwitchAccountEntity
import io.rudione.chatone.data.remote.TwitchApiClient
import io.rudione.chatone.domain.model.TwitchAccount
import io.rudione.chatone.util.Result
import io.rudione.chatone.util.map
import io.rudione.chatone.util.security.SecretVault
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import io.rudione.chatone.util.concurrent.IoDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlin.time.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface AuthRepository {
    suspend fun authenticateWithToken(accessToken: String): Result<TwitchAccount>
    suspend fun validateToken(account: TwitchAccount): Result<Boolean>
    suspend fun revokeToken(account: TwitchAccount): Result<Unit>
    suspend fun saveAccount(account: TwitchAccount)
    suspend fun getAccounts(): Flow<List<TwitchAccount>>
    suspend fun getAccountById(userId: String): TwitchAccount?
    suspend fun deleteAccount(userId: String)
    suspend fun getFirstValidAccount(): TwitchAccount?
}

class AuthRepositoryImpl(
    private val apiClient: TwitchApiClient,
    private val database: ChatoneDatabase,
    private val clientId: String
) : AuthRepository {

    companion object {
        private const val TAG = "AuthRepository"
    }

    private var legacyTokensMigrated = false

    private fun toAccount(entity: TwitchAccountEntity) = TwitchAccount(
        userId = entity.userId,
        login = entity.login,
        displayName = entity.displayName,
        profileImageUrl = entity.profileImageUrl,
        accessToken = SecretVault.open(entity.accessToken),
        refreshToken = SecretVault.open(entity.refreshToken),
        expiresAt = entity.expiresAt,
        scopes = try {
            Json.decodeFromString<List<String>>(entity.scopes)
        } catch (e: Exception) {
            emptyList()
        }
    )

    private fun migrateLegacyTokens() {
        if (legacyTokensMigrated) return
        legacyTokensMigrated = true
        try {
            database.twitchAccountQueries.getAllAccounts().executeAsList().forEach { entity ->
                if (entity.accessToken.isNotEmpty() && !SecretVault.isSealed(entity.accessToken)) {
                    database.twitchAccountQueries.updateTokens(
                        accessToken = SecretVault.seal(entity.accessToken),
                        refreshToken = SecretVault.seal(entity.refreshToken),
                        expiresAt = entity.expiresAt,
                        userId = entity.userId
                    )
                    Napier.d("Token for ${entity.login} moved to encrypted storage", tag = TAG)
                }
            }
        } catch (e: Exception) {
            Napier.w("Token migration skipped: ${e.message}", tag = TAG)
        }
    }

    override suspend fun authenticateWithToken(accessToken: String): Result<TwitchAccount> {
        return try {

            val validateResult = apiClient.validateToken(accessToken)
            if (validateResult !is Result.Success) {
                return Result.Error(
                    validateResult.exceptionOrNull() ?: Exception("Token validation failed")
                )
            }

            val validateData = validateResult.data
            val expiresAt = Clock.System.now().toEpochMilliseconds() + (validateData.expiresIn * 1000L)

            val userResult = apiClient.getUsers(accessToken = accessToken)
            if (userResult !is Result.Success || userResult.data.data.isEmpty()) {
                return Result.Error(
                    userResult.exceptionOrNull() ?: Exception("Failed to get user info")
                )
            }

            val userData = userResult.data.data.first()

            val account = TwitchAccount(
                userId = userData.id,
                login = userData.login,
                displayName = userData.displayName,
                profileImageUrl = userData.profileImageUrl,
                accessToken = accessToken,
                refreshToken = "",
                expiresAt = expiresAt,
                scopes = validateData.scopes
            )

            saveAccount(account)

            Napier.d("Authentication successful for user: ${account.login}", tag = TAG)
            Result.Success(account)
        } catch (e: Exception) {
            Napier.e("Authentication failed: ${e.message}", e, tag = TAG)
            Result.Error(e)
        }
    }

    override suspend fun validateToken(account: TwitchAccount): Result<Boolean> {
        return apiClient.validateToken(account.accessToken).map { true }
    }

    override suspend fun revokeToken(account: TwitchAccount): Result<Unit> {
        return try {
            apiClient.revokeToken(account.accessToken)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun saveAccount(account: TwitchAccount) {
        database.twitchAccountQueries.insertAccount(
            userId = account.userId,
            login = account.login,
            displayName = account.displayName,
            profileImageUrl = account.profileImageUrl,
            accessToken = SecretVault.seal(account.accessToken),
            refreshToken = SecretVault.seal(account.refreshToken),
            expiresAt = account.expiresAt,
            scopes = Json.encodeToString(account.scopes)
        )
        Napier.d("Account saved: ${account.login}", tag = TAG)
    }

    override suspend fun getAccounts(): Flow<List<TwitchAccount>> =
        database.twitchAccountQueries.getAllAccounts()
            .asFlow()
            .onStart { migrateLegacyTokens() }
            .mapToList(IoDispatcher)
            .map { entities -> entities.map(::toAccount) }

    override suspend fun getAccountById(userId: String): TwitchAccount? {
        migrateLegacyTokens()
        return database.twitchAccountQueries.getAccountById(userId)
            .executeAsOneOrNull()
            ?.let(::toAccount)
    }

    override suspend fun deleteAccount(userId: String) {
        database.twitchAccountQueries.deleteAccount(userId)
        Napier.d("Account deleted: $userId", tag = TAG)
    }

    override suspend fun getFirstValidAccount(): TwitchAccount? {
        migrateLegacyTokens()
        val accounts = database.twitchAccountQueries.getAllAccounts().executeAsList()
        for (entity in accounts) {
            val account = toAccount(entity)
            when (val result = apiClient.validateToken(account.accessToken)) {
                is Result.Success -> return account
                is Result.Error -> {
                    val msg = result.exception.message ?: ""
                    val explicitlyRejected = msg.contains("401") || msg.contains("403")
                    if (!explicitlyRejected) {
                        Napier.w(
                            "Token validation unreachable (${msg.take(80)}) — keeping ${account.login} optimistically",
                            tag = TAG
                        )
                        return account
                    }
                    Napier.w("Token for ${account.login} rejected by Twitch, trying next account", tag = TAG)
                }
                else -> return account
            }
        }
        return null
    }
}
