package com.adhamamr.passwordy

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.adhamamr.passwordy.crash.CrashReporting
import com.adhamamr.passwordy.data.local.TokenManager
import com.adhamamr.passwordy.data.network.RetrofitInstance
import com.adhamamr.passwordy.security.AppLockManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Initialises the networking layer (which needs a context for the token store) once at startup,
 * wires the inactivity auto-lock: process foreground/background events drive [AppLockManager],
 * and the stored-token flow keeps it armed only while a session exists — and starts crash/ANR
 * reporting ([CrashReporting], a no-op without a configured DSN or in debug builds).
 */
class PasswordyApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        CrashReporting.init(this)
        RetrofitInstance.init(this)

        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) = AppLockManager.onForeground()
            override fun onStop(owner: LifecycleOwner) = AppLockManager.onBackground()
        })

        val tokenManager = TokenManager(this)
        appScope.launch {
            tokenManager.token.collect { token ->
                AppLockManager.onSessionChanged(token != null)
            }
        }
    }
}
