package com.adhamamr.passwordy.ui.passwords

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adhamamr.passwordy.data.local.TokenManager
import com.adhamamr.passwordy.data.model.PasswordResponse
import com.adhamamr.passwordy.data.network.RetrofitInstance
import com.adhamamr.passwordy.data.repository.AuthRepository
import com.adhamamr.passwordy.data.repository.PasswordRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordListScreen(
    onLogout: () -> Unit,
    onAddPassword: () -> Unit,
    onPasswordClick: (Long) -> Unit,
    onOpenTwoFactor: () -> Unit = {},
    context: Context = LocalContext.current
) {
    // Setup ViewModel
    val tokenManager = remember { TokenManager(context) }
    val apiService = RetrofitInstance.api
    val repository = remember { PasswordRepository(apiService) }
    val viewModel: PasswordViewModel = viewModel(
        factory = PasswordViewModelFactory(repository)
    )

    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    var showDeleteAccountDialog by remember { mutableStateOf(false) }

    if (showDeleteAccountDialog) {
        DeleteAccountDialog(
            onDismiss = { showDeleteAccountDialog = false },
            onConfirmed = {
                // Account already deleted server-side; clear local session and return to login.
                scope.launch {
                    tokenManager.clearToken()
                    showDeleteAccountDialog = false
                    onLogout()
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Passwords") },
                actions = {
                    // Two-factor settings
                    IconButton(onClick = onOpenTwoFactor) {
                        Icon(Icons.Default.Lock, contentDescription = "Two-factor authentication")
                    }
                    // Delete account
                    IconButton(onClick = { showDeleteAccountDialog = true }) {
                        Icon(Icons.Default.DeleteForever, contentDescription = "Delete account")
                    }
                    // Logout button
                    IconButton(
                        onClick = {
                            scope.launch {
                                // Revoke the refresh token server-side, then clear locally.
                                tokenManager.refreshToken.first()?.let { rt ->
                                    runCatching { AuthRepository().logout(rt) }
                                }
                                tokenManager.clearToken()
                                onLogout()
                            }
                        }
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddPassword
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Password")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is PasswordUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is PasswordUiState.Success -> {
                    if (state.passwords.isEmpty()) {
                        // Empty state
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "No passwords saved yet",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tap the + button to add your first password",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        // Password list
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.passwords) { password ->
                                PasswordItem(
                                    password = password,
                                    onClick = { onPasswordClick(password.id) }
                                )
                            }
                        }
                    }
                }

                is PasswordUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Error loading passwords",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadPasswords() }) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Confirmation dialog for permanent account deletion. Requires the master password to be
 * re-entered (the backend re-verifies it), warns that the action is irreversible, and calls
 * [onConfirmed] only after the server confirms the deletion.
 */
@Composable
private fun DeleteAccountDialog(
    onDismiss: () -> Unit,
    onConfirmed: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var deleting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!deleting) onDismiss() },
        title = { Text("Delete account?") },
        text = {
            Column {
                Text(
                    "This permanently deletes your account and all saved passwords. " +
                        "This cannot be undone. Enter your master password to confirm."
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; error = null },
                    label = { Text("Master password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    isError = error != null,
                    enabled = !deleting,
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !deleting && password.isNotBlank(),
                onClick = {
                    scope.launch {
                        deleting = true
                        error = null
                        try {
                            val response = AuthRepository().deleteAccount(password)
                            if (response.isSuccessful) {
                                onConfirmed()
                            } else {
                                error = parseDeleteError(response.errorBody()?.string())
                                deleting = false
                            }
                        } catch (e: Exception) {
                            error = e.message ?: "Network error"
                            deleting = false
                        }
                    }
                }
            ) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(enabled = !deleting, onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/** Pulls the {"error": "..."} message from a backend error body, falling back to a generic line. */
private fun parseDeleteError(body: String?): String {
    val fallback = "Couldn't delete account"
    return try {
        if (!body.isNullOrBlank()) JSONObject(body).optString("error", fallback) else fallback
    } catch (e: Exception) {
        fallback
    }
}

@Composable
fun PasswordItem(
    password: PasswordResponse,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Label
            Text(
                text = password.label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Username
            if (!password.username.isNullOrBlank()) {
                Text(
                    text = password.username,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Category badge
            if (!password.category.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = password.category,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}