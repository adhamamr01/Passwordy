package com.adhamamr.passwordy.data.network

import com.adhamamr.passwordy.data.local.TokenManager
import com.adhamamr.passwordy.data.model.RefreshRequest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * Silent refresh: when a request comes back 401 (access token expired), exchange the stored
 * refresh token for a new access token via [refreshApi] (a plain client with no authenticator,
 * so the refresh call can't recurse) and retry the original request with the new token.
 *
 * If another thread already refreshed (the stored token differs from the one that failed) we
 * just retry with the current token. If refresh fails, tokens are cleared and we give up (the
 * 401 propagates → the user is sent back to login). Retries are capped to avoid loops.
 */
class TokenAuthenticator(
    private val tokenManager: TokenManager,
    private val refreshApi: ApiService
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= 2) return null  // already retried once — stop

        synchronized(this) {
            val failedAuth = response.request.header("Authorization")
            val current = runBlocking { tokenManager.token.first() }
            // Another request refreshed already → retry with the fresh token.
            if (current != null && "Bearer $current" != failedAuth) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $current").build()
            }

            val refresh = runBlocking { tokenManager.refreshToken.first() } ?: return null
            val refreshed = runBlocking { runCatching { refreshApi.refresh(RefreshRequest(refresh)) }.getOrNull() }
            val body = refreshed?.body()
            if (refreshed == null || !refreshed.isSuccessful || body?.token == null) {
                runBlocking { tokenManager.clearToken() }
                return null
            }

            runBlocking {
                tokenManager.saveToken(body.token)
                body.refreshToken?.let { tokenManager.saveRefreshToken(it) }
            }
            return response.request.newBuilder()
                .header("Authorization", "Bearer ${body.token}").build()
        }
    }

    private fun responseCount(response: Response): Int {
        var prior = response.priorResponse
        var count = 1
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
