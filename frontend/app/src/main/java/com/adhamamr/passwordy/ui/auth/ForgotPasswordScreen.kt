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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * "Forgot password" — collects the account email and asks the backend to email a reset token.
 * The response is deliberately generic (it never reveals whether the email is registered).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    onNavigateToReset: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsState()
    val sent = uiState as? AuthUiState.Registered

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reset password") },
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
            Text(
                "Enter your email and we'll send a reset link if an account exists.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState !is AuthUiState.Loading
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { if (email.isNotBlank()) viewModel.forgotPassword(email) },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState !is AuthUiState.Loading && email.isNotBlank()
            ) {
                if (uiState is AuthUiState.Loading) {
                    CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Send reset link")
                }
            }

            if (sent != null) {
                Spacer(Modifier.height(16.dp))
                Text(sent.message, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
            }
            if (uiState is AuthUiState.Error) {
                Spacer(Modifier.height(16.dp))
                Text((uiState as AuthUiState.Error).message, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { viewModel.resetState(); onNavigateToReset() }) {
                Text("I have a reset token")
            }
        }
    }
}
