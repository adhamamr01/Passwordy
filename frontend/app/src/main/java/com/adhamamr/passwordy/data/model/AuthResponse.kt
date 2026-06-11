package com.adhamamr.passwordy.data.model

/**
 * Login / refresh response. [token] is the short-lived access JWT; [refreshToken] is the
 * long-lived opaque token used to obtain new access tokens via /api/auth/refresh.
 */
data class AuthResponse(
    val token: String,
    val refreshToken: String?,
    val username: String,
    val email: String,
    val message: String
)
