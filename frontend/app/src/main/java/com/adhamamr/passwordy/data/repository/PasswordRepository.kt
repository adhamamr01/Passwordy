package com.adhamamr.passwordy.data.repository

import com.adhamamr.passwordy.data.local.TokenManager
import com.adhamamr.passwordy.data.model.*
import com.adhamamr.passwordy.data.network.ApiService
import kotlinx.coroutines.flow.first
import retrofit2.Response

/**
 * Wraps [ApiService] for all password operations, attaching the bearer token and unwrapping
 * Retrofit [Response]s into domain values (throwing on failure) via the [unwrap] helper.
 *
 * <p>The JWT is cached in memory after its first read so each call doesn't hit DataStore
 * from disk; callers must invoke [clearTokenCache] on logout so a stale token isn't reused.
 */
class PasswordRepository(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) {

    private var cachedToken: String? = null

    /** Returns the `Authorization` header value, reading the token from DataStore once and caching it. */
    private suspend fun bearerToken(): String {
        val token = cachedToken ?: tokenManager.token.first()?.also { cachedToken = it }
            ?: throw Exception("No authentication token found")
        return "Bearer $token"
    }

    /** Drops the cached token so the next call re-reads from DataStore. Call on logout. */
    fun clearTokenCache() {
        cachedToken = null
    }

    suspend fun getAllPasswords(): List<PasswordResponse> =
        apiService.getAllPasswords(bearerToken()).unwrap("fetch passwords")

    suspend fun getPasswordById(id: Long): PasswordResponse =
        apiService.getPasswordById(bearerToken(), id).unwrap("fetch password")

    suspend fun savePassword(request: PasswordRequest): PasswordResponse =
        apiService.savePassword(bearerToken(), request).unwrap("save password")

    suspend fun updatePassword(id: Long, request: PasswordRequest): PasswordResponse =
        apiService.updatePassword(bearerToken(), id, request).unwrap("update password")

    suspend fun deletePassword(id: Long) {
        val response = apiService.deletePassword(bearerToken(), id)
        if (!response.isSuccessful) throw Exception("Failed to delete password: ${response.message()}")
    }

    suspend fun decryptPassword(id: Long): String {
        val body = apiService.decryptPassword(bearerToken(), id).unwrap("decrypt password")
        return body["password"] ?: throw Exception("Password key not found in decrypt response")
    }

    suspend fun generatePassword(request: PasswordGenerationRequest): GeneratedPasswordResponse =
        apiService.generatePassword(request).unwrap("generate password")

    suspend fun generatePin(request: PinGenerationRequest): GeneratedPinResponse =
        apiService.generatePin(request).unwrap("generate PIN")

    suspend fun getCategories(): List<String> =
        apiService.getCategories(bearerToken()).unwrap("fetch categories")
}

private fun <T> Response<T>.unwrap(action: String): T {
    if (isSuccessful && body() != null) return body()!!
    throw Exception("Failed to $action: ${message()}")
}
