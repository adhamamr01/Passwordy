package com.adhamamr.passwordy.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.adhamamr.passwordy.data.local.TokenManager
import com.adhamamr.passwordy.data.model.MessageResponse
import com.adhamamr.passwordy.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.Response

/**
 * Backs the login and register screens. On a successful response it persists the returned
 * JWT and username via [TokenManager], then exposes the outcome through [uiState]. The
 * shared [authenticate] helper holds the identical request/response handling that login and
 * register would otherwise duplicate.
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AuthRepository()
    private val tokenManager = TokenManager(application)

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Initial)
    val uiState: StateFlow<AuthUiState> = _uiState

    /**
     * Registration no longer logs the user in: the account must be verified via the emailed
     * link first. Like forgot/reset-password, it surfaces a generic message (no token saved)
     * via the [AuthUiState.Registered] state, which the UI shows before routing back to login.
     */
    fun register(username: String, email: String, masterPassword: String) =
        submitForMessage("Registration failed") { repository.register(username, email, masterPassword) }

    fun forgotPassword(email: String) =
        submitForMessage("Couldn't start password reset") { repository.forgotPassword(email) }

    fun resetPassword(token: String, newPassword: String) =
        submitForMessage("Password reset failed") { repository.resetPassword(token, newPassword) }

    /** Runs a message-only auth action (register/forgot/reset): no token, just a [Registered] message. */
    private fun submitForMessage(errorFallback: String, action: suspend () -> Response<MessageResponse>) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val response = action()
                if (response.isSuccessful) {
                    _uiState.value = AuthUiState.Registered(
                        response.body()?.message ?: "Done — please check your email.")
                } else {
                    _uiState.value = AuthUiState.Error(parseError(response, errorFallback))
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Network error")
            }
        }
    }

    fun login(username: String, masterPassword: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val response = repository.login(username, masterPassword)
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    if (body.twoFactorRequired && body.twoFactorToken != null) {
                        _uiState.value = AuthUiState.TwoFactorRequired(body.twoFactorToken)
                    } else {
                        persistAndSucceed(body)
                    }
                } else {
                    // Surfaces backend messages like "Please verify your email before logging in".
                    _uiState.value = AuthUiState.Error(parseError(response, "Login failed"))
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Network error")
            }
        }
    }

    /** Login step 2: submit the TOTP / recovery code with the challenge token from step 1. */
    fun verifyTwoFactor(twoFactorToken: String, code: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val response = repository.verifyTwoFactor(twoFactorToken, code)
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    persistAndSucceed(body)
                } else {
                    _uiState.value = AuthUiState.Error(parseError(response, "Invalid code"))
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Network error")
            }
        }
    }

    private suspend fun persistAndSucceed(body: com.adhamamr.passwordy.data.model.AuthResponse) {
        body.token?.let { tokenManager.saveToken(it) }
        body.refreshToken?.let { tokenManager.saveRefreshToken(it) }
        tokenManager.saveUsername(body.username)
        _uiState.value = AuthUiState.Success(body.message)
    }

    /** Extracts the {"error": "..."} message from an error response body, falling back if absent. */
    private fun parseError(response: Response<*>, fallback: String): String {
        return try {
            val body = response.errorBody()?.string()
            if (!body.isNullOrBlank()) JSONObject(body).optString("error", fallback) else fallback
        } catch (e: Exception) {
            fallback
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Initial
    }
}

sealed class AuthUiState {
    object Initial : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val message: String) : AuthUiState()
    data class Registered(val message: String) : AuthUiState()
    /** Login passed the password step but needs a 2FA code; carries the challenge token. */
    data class TwoFactorRequired(val twoFactorToken: String) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}
