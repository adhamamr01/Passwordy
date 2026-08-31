package com.adhamamr.passwordy.crash

import android.content.Context
import com.adhamamr.passwordy.BuildConfig
import io.sentry.android.core.SentryAndroid

/**
 * Crash/ANR reporting (Sentry), initialised from [BuildConfig.SENTRY_DSN] — see
 * `sentry.properties.example` for how to provision it. A no-op with no configured DSN, and
 * **always** a no-op in debug builds regardless of the DSN, so a developer's local crashes are
 * never reported.
 *
 * Configured deliberately conservative for a password manager: no default PII, no screenshots,
 * no view-hierarchy dumps, no session replay, and no performance tracing (`tracesSampleRate =
 * 0.0`) — this SDK is wired for crash/ANR visibility only, not analytics. Vault contents never
 * pass through the SDK: nothing here adds the optional OkHttp/network breadcrumb integration, so
 * request/response bodies are never captured.
 */
object CrashReporting {

    fun init(context: Context) {
        val dsn = BuildConfig.SENTRY_DSN
        if (BuildConfig.DEBUG || dsn.isBlank()) {
            return
        }

        SentryAndroid.init(context) { options ->
            options.dsn = dsn
            options.environment = "release"

            // Minimize what leaves the device: no IP/device PII, no UI/screen capture.
            options.isSendDefaultPii = false
            options.isAttachScreenshot = false
            options.isAttachViewHierarchy = false

            // Crash/ANR capture only — no performance tracing or session replay.
            options.tracesSampleRate = 0.0
            options.sampleRate = 1.0
        }
    }
}
