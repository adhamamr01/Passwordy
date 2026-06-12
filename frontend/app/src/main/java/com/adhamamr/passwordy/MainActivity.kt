package com.adhamamr.passwordy

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import com.adhamamr.passwordy.security.AppLockManager
import com.adhamamr.passwordy.ui.lock.LockScreen
import com.adhamamr.passwordy.ui.navigation.AppNavigation
import com.adhamamr.passwordy.ui.theme.PasswordyTheme

// FragmentActivity (not bare ComponentActivity) so androidx BiometricPrompt can attach for the
// auto-lock gate.
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Block screenshots, screen recording, and the recents-screen thumbnail — these can
        // capture revealed/decrypted passwords. Standard hardening for a password manager.
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        enableEdgeToEdge()
        setContent {
            PasswordyTheme {
                val locked by AppLockManager.isLocked.collectAsState()
                AppNavigation()
                // Opaque overlay drawn on top of the vault whenever it's locked.
                if (locked) {
                    LockScreen(onUnlock = { AppLockManager.unlock() })
                }
            }
        }
    }
}
