package com.adhamamr.passwordy.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Completes a password reset: the user pastes the token from the email and sets a new master
 * password. On success they're directed back to login.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetPasswordScreen(
    onResetComplete: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var token by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsState()
    val done = uiState as? AuthUiState.Registered

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Set new password") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.resetState(); onNavigateBack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            OutlinedTextField(
                value = token,
                onValueChange = { token = it },
                label = { Text("Reset token") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState !is AuthUiState.Loading
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = { Text("New master password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState !is AuthUiState.Loading
            )
            Spacer(Modifier.height(24.dp))

            val valid = token.isNotBlank() && newPassword.isNotBlank()
            Button(
                onClick = { if (valid) viewModel.resetPassword(token, newPassword) },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState !is AuthUiState.Loading && valid
            ) {
                if (uiState is AuthUiState.Loading) {
                    CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Reset password")
                }
            }

            if (done != null) {
                Spacer(Modifier.height(16.dp))
                Text(done.message, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { viewModel.resetState(); onResetComplete() }) {
                    Text("Back to login")
                }
            }
            if (uiState is AuthUiState.Error) {
                Spacer(Modifier.height(16.dp))
                Text((uiState as AuthUiState.Error).message, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
