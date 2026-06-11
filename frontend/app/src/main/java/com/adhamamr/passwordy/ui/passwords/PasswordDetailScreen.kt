package com.adhamamr.passwordy.ui.passwords

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adhamamr.passwordy.data.local.TokenManager
import com.adhamamr.passwordy.data.model.PasswordResponse
import com.adhamamr.passwordy.data.network.RetrofitInstance
import com.adhamamr.passwordy.data.repository.PasswordRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordDetailScreen(
    passwordId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    context: Context = LocalContext.current
) {
    // Setup ViewModel
    val apiService = RetrofitInstance.api
    val repository = remember { PasswordRepository(apiService) }
    val viewModel: PasswordViewModel = viewModel(
        factory = PasswordViewModelFactory(repository)
    )

    val detailState by viewModel.detailState.collectAsState()
    val decryptState by viewModel.decryptState.collectAsState()
    val deleteState by viewModel.deleteState.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var decryptedPassword by remember { mutableStateOf<String?>(null) }

    // Load password on screen open
    LaunchedEffect(passwordId) {
        viewModel.loadPasswordById(passwordId)
    }

    // Handle decrypt success
    LaunchedEffect(decryptState) {
        if (decryptState is DecryptState.Success) {
            decryptedPassword = (decryptState as DecryptState.Success).decryptedPassword
        }
    }

    // Handle delete success
    LaunchedEffect(deleteState) {
        if (deleteState is DeleteState.Success) {
            Toast.makeText(context, "Password deleted", Toast.LENGTH_SHORT).show()
            viewModel.resetDeleteState()
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Password Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    // Edit button
                    IconButton(onClick = { onNavigateToEdit(passwordId) }) {
                        Icon(Icons.Default.Edit, "Edit")
                    }
                    // Delete button
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, "Delete")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = detailState) {
                is PasswordDetailState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is PasswordDetailState.Success -> {
                    val password = state.password

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Category badge
                        if (!password.category.isNullOrBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Text(
                                    text = password.category,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        // Label
                        DetailItem(
                            label = "Label",
                            value = password.label,
                            onCopy = { copyToClipboard(context, "Label", password.label) }
                        )

                        // Username
                        if (!password.username.isNullOrBlank()) {
                            DetailItem(
                                label = "Username",
                                value = password.username,
                                onCopy = { copyToClipboard(context, "Username", password.username) }
                            )
                        }

                        // Password (encrypted/decrypted)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "Password",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = decryptedPassword ?: "••••••••",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Show/Hide button
                                        if (decryptedPassword == null) {
                                            IconButton(
                                                onClick = { viewModel.decryptPassword(passwordId) },
                                                enabled = decryptState !is DecryptState.Loading
                                            ) {
                                                if (decryptState is DecryptState.Loading) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                } else {
                                                    Icon(
                                                        Icons.Default.Visibility,
                                                        "Show password"
                                                    )
                                                }
                                            }
                                        } else {
                                            IconButton(
                                                onClick = {
                                                    decryptedPassword = null
                                                    viewModel.resetDecryptState()
                                                }
                                            ) {
                                                Icon(
                                                    Icons.Default.VisibilityOff,
                                                    "Hide password"
                                                )
                                            }
                                        }

                                        // Copy button
                                        IconButton(
                                            onClick = {
                                                if (decryptedPassword != null) {
                                                    copyToClipboard(context, "Password", decryptedPassword!!)
                                                } else {
                                                    Toast.makeText(
                                                        context,
                                                        "Decrypt password first",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Default.ContentCopy, "Copy password")
                                        }
                                    }
                                }

                                // Decrypt error
                                if (decryptState is DecryptState.Error) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = (decryptState as DecryptState.Error).message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }

                        // URL
                        if (!password.url.isNullOrBlank()) {
                            DetailItem(
                                label = "URL",
                                value = password.url,
                                onCopy = { copyToClipboard(context, "URL", password.url) }
                            )
                        }

                        // Notes
                        if (!password.notes.isNullOrBlank()) {
                            Card(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Notes",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        IconButton(
                                            onClick = { copyToClipboard(context, "Notes", password.notes) }
                                        ) {
                                            Icon(
                                                Icons.Default.ContentCopy,
                                                "Copy notes",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = password.notes,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }

                        // Timestamps
                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        Text(
                            text = "Created: ${password.createdAt}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = "Updated: ${password.updatedAt}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                is PasswordDetailState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Error loading password",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadPasswordById(passwordId) }) {
                            Text("Retry")
                        }
                    }
                }

                PasswordDetailState.Idle -> {
                    // Initial state, loading will start via LaunchedEffect
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Password") },
            text = { Text("Are you sure you want to delete this password? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deletePassword(passwordId)
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete error snack bar
    if (deleteState is DeleteState.Error) {
        LaunchedEffect(deleteState) {
            Toast.makeText(
                context,
                (deleteState as DeleteState.Error).message,
                Toast.LENGTH_LONG
            ).show()
            viewModel.resetDeleteState()
        }
    }
}

@Composable
fun DetailItem(
    label: String,
    value: String,
    onCopy: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(onClick = onCopy) {
                    Icon(
                        Icons.Default.ContentCopy,
                        "Copy $label",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private const val CLIPBOARD_CLEAR_DELAY_MS = 45_000L

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text).apply {
        // Mark sensitive so Android 13+ masks the clipboard preview and excludes the value
        // from clipboard history / cross-device sync (Gboard, OEM clipboards).
        description.extras = PersistableBundle().apply {
            putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
        }
    }
    clipboard.setPrimaryClip(clip)

    // Auto-clear after a short window so a copied secret doesn't linger — but only if the
    // clipboard still holds this value (don't wipe something the user copied afterwards).
    Handler(Looper.getMainLooper()).postDelayed({
        if (clipboard.primaryClip?.getItemAt(0)?.text == text) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                clipboard.clearPrimaryClip()
            } else {
                clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
            }
        }
    }, CLIPBOARD_CLEAR_DELAY_MS)

    Toast.makeText(context, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
}