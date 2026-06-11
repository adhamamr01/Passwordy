package com.adhamamr.passwordy.data.model

/** Carries the refresh token to /api/auth/refresh (new access token) and /api/auth/logout (revoke). */
data class RefreshRequest(val refreshToken: String)
