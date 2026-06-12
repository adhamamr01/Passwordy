package com.adhamamr.passwordy.data.network

import android.content.Context
import com.adhamamr.passwordy.BuildConfig
import com.adhamamr.passwordy.data.local.TokenManager
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
 * host alias over HTTP; release → the production HTTPS backend).
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

    private fun baseClientBuilder(): OkHttpClient.Builder = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)

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
