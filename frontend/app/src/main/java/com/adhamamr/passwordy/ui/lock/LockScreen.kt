package com.adhamamr.passwordy.ui.lock

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Full-screen opaque overlay shown when the vault is locked (see [com.adhamamr.passwordy.security.AppLockManager]).
 * It immediately raises a biometric / device-credential prompt and calls [onUnlock] on success;
 * a manual "Unlock" button re-prompts after a cancel/error. If the device has no biometric and no
 * secure lock screen there is nothing to verify against, so the gate releases automatically.
 *
 * Being opaque, it also hides the vault contents behind it while locked (FLAG_SECURE already
 * blocks screenshots/recents).
 */
@Composable
fun LockScreen(onUnlock: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val authenticators = BIOMETRIC_WEAK or DEVICE_CREDENTIAL

    fun prompt() {
        if (activity == null) {
            onUnlock()
            return
        }
        if (BiometricManager.from(activity).canAuthenticate(authenticators)
            != BiometricManager.BIOMETRIC_SUCCESS
        ) {
            // No biometric enrolled and no device lock — we can't enforce a gate, so release it.
            onUnlock()
            return
        }
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onUnlock()
                }
                // On error/cancel we stay locked; the user can retry via the button.
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Passwordy")
            .setSubtitle("Confirm it's you to view your vault")
            .setAllowedAuthenticators(authenticators)
            .build()
        prompt.authenticate(info)
    }

    LaunchedEffect(Unit) { prompt() }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Filled.Lock,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Passwordy is locked",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                text = "Unlock to view your vault.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
            Button(
                onClick = { prompt() },
                modifier = Modifier.padding(top = 24.dp)
            ) {
                Text("Unlock")
            }
        }
    }
}
