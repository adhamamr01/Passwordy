package com.adhamamr.passwordy.data.network

import android.content.Context
import android.net.Uri
import com.adhamamr.passwordy.BuildConfig
import com.adhamamr.passwordy.data.local.TokenManager
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton Retrofit/OkHttp setup. [init] must be called once (from the Application) with a
 * context so the auth layer can reach [TokenManager].
 *
 * Two clients are built: a plain one used only for token refresh (no authenticator, so it can't
 * recurse on 401), and the main [api] which attaches the access token ([AuthInterceptor]) and
 * silently refreshes it on 401 ([TokenAuthenticator]).
 *
 * [BASE_URL] is supplied per build type via `BuildConfig` (debug → the emulator's `10.0.2.2`
 * host alias over HTTP; release → the production HTTPS backend). Both clients are certificate-
 * pinned to that host when [BuildConfig.CERT_PIN_PRIMARY]/[BuildConfig.CERT_PIN_BACKUP] are
 * configured (see `cert-pins.properties.example`) — a defense against MITM via a rogue/compelled
 * CA. Pinning is skipped when unconfigured, which is always true for debug.
 */
object RetrofitInstance {

    private const val BASE_URL = BuildConfig.BASE_URL

    private lateinit var tokenManager: TokenManager

    fun init(context: Context) {
        tokenManager = TokenManager(context.applicationContext)
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    /** Built only when both pins are configured; null (no pinning) otherwise. */
    private val certificatePinner: CertificatePinner? by lazy {
        val primary = BuildConfig.CERT_PIN_PRIMARY
        val backup = BuildConfig.CERT_PIN_BACKUP
        if (primary.isBlank() || backup.isBlank()) {
            null
        } else {
            val host = Uri.parse(BASE_URL).host
            if (host.isNullOrBlank()) {
                null
            } else {
                CertificatePinner.Builder()
                    .add(host, primary, backup)
                    .build()
            }
        }
    }

    private fun baseClientBuilder(): OkHttpClient.Builder = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .apply { certificatePinner?.let { certificatePinner(it) } }

    private fun retrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    /** Plain client used only for /api/auth/refresh — no auth interceptor/authenticator. */
    private val refreshApi: ApiService by lazy {
        retrofit(baseClientBuilder().build()).create(ApiService::class.java)
    }

    val api: ApiService by lazy {
        val client = baseClientBuilder()
            .addInterceptor(AuthInterceptor(tokenManager))
            .authenticator(TokenAuthenticator(tokenManager, refreshApi))
            .build()
        retrofit(client).create(ApiService::class.java)
    }
}
