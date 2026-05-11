package com.adhamamr.passwordy.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.adhamamr.passwordy.data.local.TokenManager
import com.adhamamr.passwordy.data.model.AuthResponse
import com.adhamamr.passwordy.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.Response

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AuthRepository()
    private val tokenManager = TokenManager(application)

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Initial)
    val uiState: StateFlow<AuthUiState> = _uiState

    fun register(username: String, email: String, masterPassword: String) {
        authenticate(errorFallback = "Registration failed") {
            repository.register(username, email, masterPassword)
        }
    }

    fun login(username: String, masterPassword: String) {
        authenticate(errorFallback = "Login failed") {
            repository.login(username, masterPassword)
        }
    }

    private fun authenticate(errorFallback: String, action: suspend () -> Response<AuthResponse>) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val response = action()
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    tokenManager.saveToken(body.token)
                    tokenManager.saveUsername(body.username)
                    _uiState.value = AuthUiState.Success(body.message)
                } else {
                    _uiState.value = AuthUiState.Error(response.message() ?: errorFallback)
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Network error")
            }
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
    data class Error(val message: String) : AuthUiState()
}
