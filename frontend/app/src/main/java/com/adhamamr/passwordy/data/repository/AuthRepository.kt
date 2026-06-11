package com.adhamamr.passwordy.data.repository

import com.adhamamr.passwordy.data.model.AuthResponse
import com.adhamamr.passwordy.data.model.ForgotPasswordRequest
import com.adhamamr.passwordy.data.model.LoginRequest
import com.adhamamr.passwordy.data.model.MessageResponse
import com.adhamamr.passwordy.data.model.RefreshRequest
import com.adhamamr.passwordy.data.model.RegisterRequest
import com.adhamamr.passwordy.data.model.ResetPasswordRequest
import com.adhamamr.passwordy.data.model.TotpCodeRequest
import com.adhamamr.passwordy.data.model.TotpEnableResponse
import com.adhamamr.passwordy.data.model.TotpSetupResponse
import com.adhamamr.passwordy.data.model.TwoFactorVerifyRequest
import com.adhamamr.passwordy.data.network.RetrofitInstance
import retrofit2.Response

/** Thin wrapper over the auth endpoints; returns the raw Retrofit [Response] for the ViewModel to handle. */
class AuthRepository {

    private val api = RetrofitInstance.api

    suspend fun register(username: String, email: String, masterPassword: String): Response<MessageResponse> {
        val request = RegisterRequest(username, email, masterPassword)
        return api.register(request)
    }

    suspend fun login(username: String, masterPassword: String): Response<AuthResponse> {
        val request = LoginRequest(username, masterPassword)
        return api.login(request)
    }

    suspend fun forgotPassword(email: String): Response<MessageResponse> {
        return api.forgotPassword(ForgotPasswordRequest(email))
    }

    suspend fun resetPassword(token: String, newPassword: String): Response<MessageResponse> {
        return api.resetPassword(ResetPasswordRequest(token, newPassword))
    }

    /** Revokes the refresh token server-side (logout). Best-effort; the caller still clears locally. */
    suspend fun logout(refreshToken: String): Response<MessageResponse> {
        return api.logout(RefreshRequest(refreshToken))
    }

    suspend fun verifyTwoFactor(twoFactorToken: String, code: String): Response<AuthResponse> {
        return api.verifyTwoFactor(TwoFactorVerifyRequest(twoFactorToken, code))
    }

    suspend fun setupTotp(): Response<TotpSetupResponse> = api.setupTotp()

    suspend fun enableTotp(code: String): Response<TotpEnableResponse> = api.enableTotp(TotpCodeRequest(code))

    suspend fun disableTotp(code: String): Response<MessageResponse> = api.disableTotp(TotpCodeRequest(code))
}