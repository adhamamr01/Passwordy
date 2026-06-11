package com.adhamamr.passwordy.data.network

import com.adhamamr.passwordy.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton Retrofit/OkHttp setup exposing the lazily-built [api].
 *
 * [BASE_URL] defaults to `10.0.2.2` — the Android emulator's alias for the host machine's
 * `localhost`. For a physical device, change it to the host's LAN IP (e.g. 192.168.x.x).
 */
object RetrofitInstance {

    private const val BASE_URL = "http://10.0.2.2:8080/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        // Bodies carry master passwords, decrypted secrets, and bearer tokens — never log them
        // in release builds. Full bodies are visible only in debug builds.
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}