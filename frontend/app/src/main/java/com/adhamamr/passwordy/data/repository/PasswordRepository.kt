package com.adhamamr.passwordy.data.repository

import com.adhamamr.passwordy.data.model.*
import com.adhamamr.passwordy.data.network.ApiService
import retrofit2.Response

/**
 * Wraps [ApiService] for all password operations and unwraps Retrofit [Response]s into domain
 * values (throwing on failure) via [unwrap]. The access token is attached automatically by the
 * network layer's `AuthInterceptor` (and refreshed on 401 by `TokenAuthenticator`), so this
 * layer no longer threads the bearer token through every call.
 */
class PasswordRepository(
    private val apiService: ApiService
) {

    suspend fun getAllPasswords(): List<PasswordResponse> =
        apiService.getAllPasswords().unwrap("fetch passwords")

    suspend fun getPasswordById(id: Long): PasswordResponse =
        apiService.getPasswordById(id).unwrap("fetch password")

    suspend fun savePassword(request: PasswordRequest): PasswordResponse =
        apiService.savePassword(request).unwrap("save password")

    suspend fun updatePassword(id: Long, request: PasswordRequest): PasswordResponse =
        apiService.updatePassword(id, request).unwrap("update password")

    suspend fun deletePassword(id: Long) {
        val response = apiService.deletePassword(id)
        if (!response.isSuccessful) throw Exception("Failed to delete password: ${response.message()}")
    }

    suspend fun decryptPassword(id: Long): String {
        val body = apiService.decryptPassword(id).unwrap("decrypt password")
        return body["password"] ?: throw Exception("Password key not found in decrypt response")
    }

    suspend fun generatePassword(request: PasswordGenerationRequest): GeneratedPasswordResponse =
        apiService.generatePassword(request).unwrap("generate password")

    suspend fun generatePin(request: PinGenerationRequest): GeneratedPinResponse =
        apiService.generatePin(request).unwrap("generate PIN")

    suspend fun getCategories(): List<String> =
        apiService.getCategories().unwrap("fetch categories")
}

private fun <T> Response<T>.unwrap(action: String): T {
    if (isSuccessful && body() != null) return body()!!
    throw Exception("Failed to $action: ${message()}")
}
