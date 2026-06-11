package com.adhamamr.passwordy.data.network

import com.adhamamr.passwordy.data.local.TokenManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Attaches the current access token as `Authorization: Bearer <token>` to authenticated
 * requests. Public routes (auth + password generation) are left untouched. Reading the token is
 * a fast DataStore lookup; [runBlocking] is acceptable on OkHttp's dispatcher threads.
 */
class AuthInterceptor(private val tokenManager: TokenManager) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath
        if (path.contains("/api/auth/") || path.contains("/api/password/generate")) {
            return chain.proceed(request)
        }
        val token = runBlocking { tokenManager.token.first() }
        val authed = if (token != null) {
            request.newBuilder().header("Authorization", "Bearer $token").build()
        } else {
            request
        }
        return chain.proceed(authed)
    }
}
