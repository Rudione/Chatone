package io.rudione.chatone.data.repository

import io.github.aakira.napier.Napier
import io.rudione.chatone.data.local.ChatoneDatabase
import io.rudione.chatone.data.remote.TwitchApiClient
import io.rudione.chatone.domain.model.TwitchAccount
import io.rudione.chatone.util.Result
import io.rudione.chatone.util.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock
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

    override suspend fun authenticateWithToken(accessToken: String): Result<TwitchAccount> {
        return try {
            // Validate the token first
            val validateResult = apiClient.validateToken(accessToken)
            if (validateResult !is Result.Success) {
                return Result.Error(Exception("Token validation failed"))
            }

            val validateData = validateResult.data
            val expiresAt = Clock.System.now().toEpochMilliseconds() + (validateData.expiresIn * 1000L)

            // Get user info
            val userResult = apiClient.getUsers(accessToken = accessToken)
            if (userResult !is Result.Success || userResult.data.data.isEmpty()) {
                return Result.Error(Exception("Failed to get user info"))
            }

            val userData = userResult.data.data.first()

            val account = TwitchAccount(
                userId = userData.id,
                login = userData.login,
                displayName = userData.displayName,
                profileImageUrl = userData.profileImageUrl,
                accessToken = accessToken,
                refreshToken = "", // Implicit grant has no refresh token
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
            accessToken = account.accessToken,
            refreshToken = account.refreshToken,
            expiresAt = account.expiresAt,
            scopes = Json.encodeToString(account.scopes)
        )
        Napier.d("Account saved: ${account.login}", tag = TAG)
    }

    override suspend fun getAccounts(): Flow<List<TwitchAccount>> = flow {
        val accounts = database.twitchAccountQueries.getAllAccounts()
            .executeAsList()
            .map { entity ->
                TwitchAccount(
                    userId = entity.userId,
                    login = entity.login,
                    displayName = entity.displayName,
                    profileImageUrl = entity.profileImageUrl,
                    accessToken = entity.accessToken,
                    refreshToken = entity.refreshToken,
                    expiresAt = entity.expiresAt,
                    scopes = try {
                        Json.decodeFromString<List<String>>(entity.scopes)
                    } catch (e: Exception) {
                        emptyList()
                    }
                )
            }
        emit(accounts)
    }

    override suspend fun getAccountById(userId: String): TwitchAccount? {
        return database.twitchAccountQueries.getAccountById(userId)
            .executeAsOneOrNull()
            ?.let { entity ->
                TwitchAccount(
                    userId = entity.userId,
                    login = entity.login,
                    displayName = entity.displayName,
                    profileImageUrl = entity.profileImageUrl,
                    accessToken = entity.accessToken,
                    refreshToken = entity.refreshToken,
                    expiresAt = entity.expiresAt,
                    scopes = try {
                        Json.decodeFromString<List<String>>(entity.scopes)
                    } catch (e: Exception) {
                        emptyList()
                    }
                )
            }
    }

    override suspend fun deleteAccount(userId: String) {
        database.twitchAccountQueries.deleteAccount(userId)
        Napier.d("Account deleted: $userId", tag = TAG)
    }

    override suspend fun getFirstValidAccount(): TwitchAccount? {
        val accounts = database.twitchAccountQueries.getAllAccounts().executeAsList()
        for (entity in accounts) {
            val account = TwitchAccount(
                userId = entity.userId,
                login = entity.login,
                displayName = entity.displayName,
                profileImageUrl = entity.profileImageUrl,
                accessToken = entity.accessToken,
                refreshToken = entity.refreshToken,
                expiresAt = entity.expiresAt,
                scopes = try {
                    Json.decodeFromString<List<String>>(entity.scopes)
                } catch (e: Exception) {
                    emptyList()
                }
            )
            val result = validateToken(account)
            if (result is Result.Success && result.data) {
                return account
            }
        }
        return null
    }
}
