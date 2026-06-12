package com.adhamamr.passwordy.security

import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Inactivity auto-lock for the vault. When a session is active and the app spends longer than
 * [LOCK_TIMEOUT_MS] in the background, the UI is locked behind a biometric / device-credential
 * gate on return (see the lock overlay in `MainActivity`).
 *
 * Driven by `ProcessLifecycleOwner` (foreground/background) from `PasswordyApp`, which also
 * keeps [sessionActive] in sync with whether a token is stored — locking is meaningless when the
 * user isn't logged in. Elapsed time uses [SystemClock.elapsedRealtime] so it can't be defeated
 * by changing the wall clock.
 */
object AppLockManager {

    /** Lock after this long in the background. */
    private const val LOCK_TIMEOUT_MS = 60_000L

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    @Volatile
    private var sessionActive = false

    /** elapsedRealtime when last backgrounded, or 0 when foregrounded / disarmed. */
    @Volatile
    private var backgroundedAt = 0L

    /** Called when the process enters the foreground. */
    fun onForeground() {
        if (sessionActive && backgroundedAt != 0L &&
            SystemClock.elapsedRealtime() - backgroundedAt >= LOCK_TIMEOUT_MS
        ) {
            _isLocked.value = true
        }
        backgroundedAt = 0L
    }

    /** Called when the process enters the background. */
    fun onBackground() {
        if (sessionActive) {
            backgroundedAt = SystemClock.elapsedRealtime()
        }
    }

    /** Called after a successful biometric / device-credential re-auth. */
    fun unlock() {
        _isLocked.value = false
    }

    /**
     * Keep the lock armed only while logged in. On logout we clear any pending lock so the login
     * screen is never shown behind the lock overlay.
     */
    fun onSessionChanged(active: Boolean) {
        sessionActive = active
        if (!active) {
            _isLocked.value = false
            backgroundedAt = 0L
        }
    }
}
