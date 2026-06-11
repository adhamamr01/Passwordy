package com.adhamamr.passwordy

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.adhamamr.passwordy.ui.navigation.AppNavigation
import com.adhamamr.passwordy.ui.theme.PasswordyTheme

class MainActivity : ComponentActivity() {
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
                AppNavigation()
            }
        }
    }
}