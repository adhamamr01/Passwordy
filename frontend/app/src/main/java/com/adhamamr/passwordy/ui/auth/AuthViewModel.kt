package com.adhamamr.passwordy.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.adhamamr.passwordy.data.local.TokenManager
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
     * link first. On success we surface the generic acknowledgement (no token is saved) so the
     * UI can tell the user to check their inbox.
     */
    fun register(username: String, email: String, masterPassword: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val response = repository.register(username, email, masterPassword)
                if (response.isSuccessful) {
                    val message = response.body()?.message
                        ?: "Account created. Check your email to verify before logging in."
                    _uiState.value = AuthUiState.Registered(message)
                } else {
                    _uiState.value = AuthUiState.Error(parseError(response, "Registration failed"))
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
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    tokenManager.saveToken(body.token)
                    tokenManager.saveUsername(body.username)
                    _uiState.value = AuthUiState.Success(body.message)
                } else {
                    // Surfaces backend messages like "Please verify your email before logging in".
                    _uiState.value = AuthUiState.Error(parseError(response, "Login failed"))
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Network error")
            }
        }
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
    data class Error(val message: String) : AuthUiState()
}
