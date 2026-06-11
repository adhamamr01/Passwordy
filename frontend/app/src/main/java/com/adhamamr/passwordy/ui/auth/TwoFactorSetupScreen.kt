package com.adhamamr.passwordy.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.adhamamr.passwordy.data.model.TotpSetupResponse
import com.adhamamr.passwordy.data.repository.AuthRepository
import kotlinx.coroutines.launch

/**
 * TOTP enrollment: requests a secret, shows it (for manual entry / QR) plus the otpauth URI,
 * confirms a code to activate, then displays the one-time recovery codes (shown once).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TwoFactorSetupScreen(onNavigateBack: () -> Unit) {
    val repository = remember { AuthRepository() }
    val scope = rememberCoroutineScope()

    var setup by remember { mutableStateOf<TotpSetupResponse?>(null) }
    var code by remember { mutableStateOf("") }
    var recoveryCodes by remember { mutableStateOf<List<String>?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        loading = true
        try {
            val response = repository.setupTotp()
            if (response.isSuccessful) setup = response.body() else message = "Couldn't start 2FA setup"
        } catch (e: Exception) {
            message = e.message ?: "Network error"
        }
        loading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Two-factor authentication") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Back") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState())
        ) {
            val codes = recoveryCodes
            if (codes != null) {
                Text("2FA is enabled.", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text("Save these recovery codes somewhere safe — each works once if you lose your authenticator:")
                Spacer(Modifier.height(8.dp))
                SelectionContainer {
                    Column { codes.forEach { Text(it, fontFamily = FontFamily.Monospace) } }
                }
                Spacer(Modifier.height(16.dp))
                Button(onClick = onNavigateBack, modifier = Modifier.fillMaxWidth()) { Text("Done") }
            } else if (loading && setup == null) {
                CircularProgressIndicator()
            } else {
                setup?.let { s ->
                    Text("1. Add this account to your authenticator app.")
                    Spacer(Modifier.height(8.dp))
                    Text("Secret (manual entry):", style = MaterialTheme.typography.labelMedium)
                    SelectionContainer { Text(s.secret, fontFamily = FontFamily.Monospace) }
                    Spacer(Modifier.height(8.dp))
                    Text("otpauth URI (for QR):", style = MaterialTheme.typography.labelMedium)
                    SelectionContainer { Text(s.otpauthUri, style = MaterialTheme.typography.bodySmall) }
                    Spacer(Modifier.height(16.dp))
                    Text("2. Enter the 6-digit code to confirm.")
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("Code") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !loading
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                loading = true
                                message = null
                                try {
                                    val response = repository.enableTotp(code.trim())
                                    if (response.isSuccessful) {
                                        recoveryCodes = response.body()?.recoveryCodes ?: emptyList()
                                    } else {
                                        message = "Invalid code — try again"
                                    }
                                } catch (e: Exception) {
                                    message = e.message ?: "Network error"
                                }
                                loading = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !loading && code.isNotBlank()
                    ) { Text("Enable 2FA") }
                }
            }

            message?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
