package com.adhamamr.passwordy.data.network

import com.adhamamr.passwordy.data.model.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit definition of the Passwordy HTTP API. Authenticated calls take the
 * `Authorization` header explicitly (supplied by the repository); auth and generation
 * endpoints omit it. Each method returns a Retrofit [Response] so callers can inspect status.
 */
interface ApiService {

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<MessageResponse>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("api/password/generate")
    suspend fun generatePassword(@Body request: PasswordGenerationRequest): Response<GeneratedPasswordResponse>

    @POST("api/password/generate-pin")
    suspend fun generatePin(@Body request: PinGenerationRequest): Response<GeneratedPinResponse>

    @GET("api/password/categories")
    suspend fun getCategories(@Header("Authorization") token: String): Response<List<String>>

    @GET("api/passwords")
    suspend fun getAllPasswords(@Header("Authorization") token: String): Response<List<PasswordResponse>>

    @POST("api/passwords")
    suspend fun savePassword(
        @Header("Authorization") token: String,
        @Body request: PasswordRequest
    ): Response<PasswordResponse>

    @GET("api/passwords/{id}")
    suspend fun getPasswordById(
        @Header("Authorization") token: String,
        @Path("id") id: Long
    ): Response<PasswordResponse>

    @POST("api/passwords/{id}/decrypt")
    suspend fun decryptPassword(
        @Header("Authorization") token: String,
        @Path("id") id: Long
    ): Response<Map<String, String>>

    @PUT("api/passwords/{id}")
    suspend fun updatePassword(
        @Header("Authorization") token: String,
        @Path("id") id: Long,
        @Body request: PasswordRequest
    ): Response<PasswordResponse>

    @DELETE("api/passwords/{id}")
    suspend fun deletePassword(
        @Header("Authorization") token: String,
        @Path("id") id: Long
    ): Response<Unit>
}