package com.adhamamr.passwordy.ui.passwords

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adhamamr.passwordy.data.model.*
import com.adhamamr.passwordy.data.repository.PasswordRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class PasswordUiState {
    object Loading : PasswordUiState()
    data class Success(val passwords: List<PasswordResponse>) : PasswordUiState()
    data class Error(val message: String) : PasswordUiState()
}

sealed class PasswordDetailState {
    object Idle : PasswordDetailState()
    object Loading : PasswordDetailState()
    data class Success(val password: PasswordResponse) : PasswordDetailState()
    data class Error(val message: String) : PasswordDetailState()
}

sealed class DecryptState {
    object Idle : DecryptState()
    object Loading : DecryptState()
    data class Success(val decryptedPassword: String) : DecryptState()
    data class Error(val message: String) : DecryptState()
}

sealed class GenerateState {
    object Idle : GenerateState()
    object Loading : GenerateState()
    data class PasswordSuccess(val password: String) : GenerateState()
    data class PinSuccess(val pin: String) : GenerateState()
    data class Error(val message: String) : GenerateState()
}

sealed class SaveState {
    object Idle : SaveState()
    object Loading : SaveState()
    object Success : SaveState()
    data class Error(val message: String) : SaveState()
}

sealed class DeleteState {
    object Idle : DeleteState()
    object Loading : DeleteState()
    object Success : DeleteState()
    data class Error(val message: String) : DeleteState()
}

class PasswordViewModel(
    private val repository: PasswordRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<PasswordUiState>(PasswordUiState.Loading)
    val uiState: StateFlow<PasswordUiState> = _uiState

    private val _detailState = MutableStateFlow<PasswordDetailState>(PasswordDetailState.Idle)
    val detailState: StateFlow<PasswordDetailState> = _detailState

    private val _decryptState = MutableStateFlow<DecryptState>(DecryptState.Idle)
    val decryptState: StateFlow<DecryptState> = _decryptState

    private val _generateState = MutableStateFlow<GenerateState>(GenerateState.Idle)
    val generateState: StateFlow<GenerateState> = _generateState

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState

    private val _deleteState = MutableStateFlow<DeleteState>(DeleteState.Idle)
    val deleteState: StateFlow<DeleteState> = _deleteState

    private val _categories = MutableStateFlow<List<String>>(emptyList())
    val categories: StateFlow<List<String>> = _categories

    init {
        loadPasswords()
        loadCategories()
    }

    fun loadPasswords() {
        viewModelScope.launch {
            _uiState.value = PasswordUiState.Loading
            try {
                _uiState.value = PasswordUiState.Success(repository.getAllPasswords())
            } catch (e: Exception) {
                _uiState.value = PasswordUiState.Error(e.message ?: "Failed to load passwords")
            }
        }
    }

    fun loadPasswordById(id: Long) {
        viewModelScope.launch {
            _detailState.value = PasswordDetailState.Loading
            try {
                _detailState.value = PasswordDetailState.Success(repository.getPasswordById(id))
            } catch (e: Exception) {
                _detailState.value = PasswordDetailState.Error(e.message ?: "Failed to load password")
            }
        }
    }

    fun decryptPassword(id: Long) {
        viewModelScope.launch {
            _decryptState.value = DecryptState.Loading
            try {
                _decryptState.value = DecryptState.Success(repository.decryptPassword(id))
            } catch (e: Exception) {
                _decryptState.value = DecryptState.Error(e.message ?: "Failed to decrypt password")
            }
        }
    }

    fun generatePassword(length: Int, includeSymbols: Boolean) {
        viewModelScope.launch {
            _generateState.value = GenerateState.Loading
            try {
                val response = repository.generatePassword(PasswordGenerationRequest(length, includeSymbols))
                _generateState.value = GenerateState.PasswordSuccess(response.password)
            } catch (e: Exception) {
                _generateState.value = GenerateState.Error(e.message ?: "Failed to generate password")
            }
        }
    }

    fun generatePin(length: Int) {
        viewModelScope.launch {
            _generateState.value = GenerateState.Loading
            try {
                val response = repository.generatePin(PinGenerationRequest(length))
                _generateState.value = GenerateState.PinSuccess(response.pin)
            } catch (e: Exception) {
                _generateState.value = GenerateState.Error(e.message ?: "Failed to generate PIN")
            }
        }
    }

    fun savePassword(label: String, username: String, password: String, url: String?, notes: String?, category: String) {
        persistPassword(errorMsg = "Failed to save password") {
            val saved = repository.savePassword(PasswordRequest(label, username, password, url, notes, category))
            val current = (_uiState.value as? PasswordUiState.Success)?.passwords ?: emptyList()
            _uiState.value = PasswordUiState.Success(current + saved)
        }
    }

    fun updatePassword(id: Long, label: String, username: String, password: String, url: String?, notes: String?, category: String) {
        persistPassword(errorMsg = "Failed to update password") {
            val updated = repository.updatePassword(id, PasswordRequest(label, username, password, url, notes, category))
            val current = (_uiState.value as? PasswordUiState.Success)?.passwords ?: emptyList()
            _uiState.value = PasswordUiState.Success(current.map { if (it.id == id) updated else it })
        }
    }

    fun deletePassword(id: Long) {
        viewModelScope.launch {
            _deleteState.value = DeleteState.Loading
            try {
                repository.deletePassword(id)
                _deleteState.value = DeleteState.Success
                val current = (_uiState.value as? PasswordUiState.Success)?.passwords ?: emptyList()
                _uiState.value = PasswordUiState.Success(current.filterNot { it.id == id })
            } catch (e: Exception) {
                _deleteState.value = DeleteState.Error(e.message ?: "Failed to delete password")
            }
        }
    }

    private fun persistPassword(errorMsg: String, action: suspend () -> Unit) {
        viewModelScope.launch {
            _saveState.value = SaveState.Loading
            try {
                action()
                _saveState.value = SaveState.Success
            } catch (e: Exception) {
                _saveState.value = SaveState.Error(e.message ?: errorMsg)
            }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            try {
                _categories.value = repository.getCategories()
            } catch (e: Exception) {
                _categories.value = listOf("Social", "Work", "Finance", "Shopping", "Other")
            }
        }
    }

    fun resetDecryptState() { _decryptState.value = DecryptState.Idle }
    fun resetGenerateState() { _generateState.value = GenerateState.Idle }
    fun resetSaveState() { _saveState.value = SaveState.Idle }
    fun resetDeleteState() { _deleteState.value = DeleteState.Idle }
    fun resetDetailState() { _detailState.value = PasswordDetailState.Idle }
}
