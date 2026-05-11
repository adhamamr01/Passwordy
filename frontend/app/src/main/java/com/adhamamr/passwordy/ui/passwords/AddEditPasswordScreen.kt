package com.adhamamr.passwordy.ui.passwords

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adhamamr.passwordy.data.local.TokenManager
import com.adhamamr.passwordy.data.network.RetrofitInstance
import com.adhamamr.passwordy.data.repository.PasswordRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditPasswordScreen(
    passwordId: Long? = null, // null = add mode, not null = edit mode
    onNavigateBack: () -> Unit,
    context: Context = LocalContext.current
) {
    // Setup ViewModel
    val tokenManager = remember { TokenManager(context) }
    val apiService = RetrofitInstance.api
    val repository = remember { PasswordRepository(apiService, tokenManager) }
    val viewModel: PasswordViewModel = viewModel(
        factory = PasswordViewModelFactory(repository)
    )

    // Form state
    var label by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showCategoryDropdown by remember { mutableStateOf(false) }
    var showGenerateDialog by remember { mutableStateOf(false) }

    // Observe states
    val categories by viewModel.categories.collectAsState()
    val saveState by viewModel.saveState.collectAsState()
    val detailState by viewModel.detailState.collectAsState()

    // Load password if editing
    LaunchedEffect(passwordId) {
        if (passwordId != null) {
            viewModel.loadPasswordById(passwordId)
        }
    }

    // Populate fields when loading password for edit
    LaunchedEffect(detailState) {
        if (detailState is PasswordDetailState.Success) {
            val pwd = (detailState as PasswordDetailState.Success).password
            label = pwd.label
            username = pwd.username ?: ""
            password = "********" // Show placeholder, will need to decrypt to see
            url = pwd.url ?: ""
            notes = pwd.notes ?: ""
            selectedCategory = pwd.category ?: categories.firstOrNull() ?: ""
        }
    }

    // Set default category
    LaunchedEffect(categories) {
        if (selectedCategory.isEmpty() && categories.isNotEmpty()) {
            selectedCategory = categories.first()
        }
    }

    // Handle save success
    LaunchedEffect(saveState) {
        if (saveState is SaveState.Success) {
            viewModel.resetSaveState()
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (passwordId == null) "Add Password" else "Edit Password") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Label field
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Label *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Username field
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Password field with visibility toggle
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password"
                        )
                    }
                }
            )

            // Generate Password/PIN Button
            OutlinedButton(
                onClick = { showGenerateDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Generate Password or PIN")
            }

            // Category dropdown
            ExposedDropdownMenuBox(
                expanded = showCategoryDropdown,
                onExpandedChange = { showCategoryDropdown = it }
            ) {
                OutlinedTextField(
                    value = selectedCategory,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryDropdown) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = showCategoryDropdown,
                    onDismissRequest = { showCategoryDropdown = false }
                ) {
                    categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category) },
                            onClick = {
                                selectedCategory = category
                                showCategoryDropdown = false
                            }
                        )
                    }
                }
            }

            // URL field (optional)
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("URL") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Notes field (optional)
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )

            val isFormValid = label.isNotBlank() && username.isNotBlank() && password.isNotBlank() && selectedCategory.isNotBlank()

            // Save button
            Button(
                onClick = {
                    if (isFormValid) {
                        if (passwordId == null) {
                            viewModel.savePassword(
                                label = label,
                                username = username,
                                password = password,
                                url = url.ifBlank { null },
                                notes = notes.ifBlank { null },
                                category = selectedCategory
                            )
                        } else {
                            viewModel.updatePassword(
                                id = passwordId,
                                label = label,
                                username = username,
                                password = password,
                                url = url.ifBlank { null },
                                notes = notes.ifBlank { null },
                                category = selectedCategory
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = saveState !is SaveState.Loading && isFormValid
            ) {
                if (saveState is SaveState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(if (passwordId == null) "Save Password" else "Update Password")
                }
            }

            // Error message
            if (saveState is SaveState.Error) {
                Text(
                    text = (saveState as SaveState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    // Generate Password Dialog
    if (showGenerateDialog) {
        GeneratePasswordDialog(
            onDismiss = { showGenerateDialog = false },
            onPasswordGenerated = { generated ->
                password = generated
                showGenerateDialog = false
            },
            viewModel = viewModel
        )
    }
}

@Composable
fun GeneratePasswordDialog(
    onDismiss: () -> Unit,
    onPasswordGenerated: (String) -> Unit,
    viewModel: PasswordViewModel
) {
    var isPin by remember { mutableStateOf(false) }
    var length by remember { mutableStateOf(12) }
    var includeSymbols by remember { mutableStateOf(true) }
    var generatedValue by remember { mutableStateOf("") }

    val generateState by viewModel.generateState.collectAsState()

    LaunchedEffect(generateState) {
        when (generateState) {
            is GenerateState.PasswordSuccess -> {
                generatedValue = (generateState as GenerateState.PasswordSuccess).password
            }
            is GenerateState.PinSuccess -> {
                generatedValue = (generateState as GenerateState.PinSuccess).pin
            }
            else -> {}
        }
    }

    AlertDialog(
        onDismissRequest = {
            viewModel.resetGenerateState()
            onDismiss()
        },
        title = { Text("Generate ${if (isPin) "PIN" else "Password"}") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Toggle between Password and PIN
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FilterChip(
                        selected = !isPin,
                        onClick = {
                            isPin = false
                            length = 12
                            generatedValue = ""
                            viewModel.resetGenerateState()
                        },
                        label = { Text("Password") }
                    )
                    FilterChip(
                        selected = isPin,
                        onClick = {
                            isPin = true
                            length = 6
                            includeSymbols = false
                            generatedValue = ""
                            viewModel.resetGenerateState()
                        },
                        label = { Text("PIN") }
                    )
                }

                // Length slider
                Text("Length: $length ${if (isPin) "digits" else "characters"}")
                Slider(
                    value = length.toFloat(),
                    onValueChange = { length = it.toInt() },
                    valueRange = if (isPin) 4f..12f else 8f..32f,
                    steps = if (isPin) 7 else 23
                )

                // Include symbols toggle (only for passwords)
                if (!isPin) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Include Symbols")
                        Switch(
                            checked = includeSymbols,
                            onCheckedChange = { includeSymbols = it }
                        )
                    }
                }

                // Generate button
                Button(
                    onClick = {
                        if (isPin) {
                            viewModel.generatePin(length)
                        } else {
                            viewModel.generatePassword(length, includeSymbols)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Generate")
                }

                // Display generated value
                if (generatedValue.isNotEmpty()) {
                    OutlinedTextField(
                        value = generatedValue,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Generated ${if (isPin) "PIN" else "Password"}") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Loading indicator
                if (generateState is GenerateState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }

                // Error message
                if (generateState is GenerateState.Error) {
                    Text(
                        text = (generateState as GenerateState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (generatedValue.isNotEmpty()) {
                        onPasswordGenerated(generatedValue)
                        viewModel.resetGenerateState()
                    }
                },
                enabled = generatedValue.isNotEmpty()
            ) {
                Text("Use")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    viewModel.resetGenerateState()
                    onDismiss()
                }
            ) {
                Text("Cancel")
            }
        }
    )
}