package com.adhamamr.passwordy.data.network

import com.adhamamr.passwordy.data.model.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit definition of the Passwordy HTTP API. The access token is attached automatically by
 * [AuthInterceptor] on authenticated calls (and refreshed by [TokenAuthenticator] on a 401), so
 * methods no longer take an explicit `Authorization` header. Auth and generation endpoints are
 * public. Each method returns a Retrofit [Response] so callers can inspect status.
 */
interface ApiService {

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<MessageResponse>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("api/auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): Response<MessageResponse>

    @POST("api/auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): Response<MessageResponse>

    @POST("api/auth/refresh")
    suspend fun refresh(@Body request: RefreshRequest): Response<AuthResponse>

    @POST("api/auth/logout")
    suspend fun logout(@Body request: RefreshRequest): Response<MessageResponse>

    @POST("api/password/generate")
    suspend fun generatePassword(@Body request: PasswordGenerationRequest): Response<GeneratedPasswordResponse>

    @POST("api/password/generate-pin")
    suspend fun generatePin(@Body request: PinGenerationRequest): Response<GeneratedPinResponse>

    @GET("api/password/categories")
    suspend fun getCategories(): Response<List<String>>

    @GET("api/passwords")
    suspend fun getAllPasswords(): Response<List<PasswordResponse>>

    @POST("api/passwords")
    suspend fun savePassword(@Body request: PasswordRequest): Response<PasswordResponse>

    @GET("api/passwords/{id}")
    suspend fun getPasswordById(@Path("id") id: Long): Response<PasswordResponse>

    @POST("api/passwords/{id}/decrypt")
    suspend fun decryptPassword(@Path("id") id: Long): Response<Map<String, String>>

    @PUT("api/passwords/{id}")
    suspend fun updatePassword(@Path("id") id: Long, @Body request: PasswordRequest): Response<PasswordResponse>

    @DELETE("api/passwords/{id}")
    suspend fun deletePassword(@Path("id") id: Long): Response<Unit>
}
