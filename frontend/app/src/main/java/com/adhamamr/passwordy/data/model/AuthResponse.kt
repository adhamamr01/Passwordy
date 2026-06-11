package com.adhamamr.passwordy.data.model

/**
 * Login / refresh response. [token] is the short-lived access JWT; [refreshToken] is the
 * long-lived opaque token. When [twoFactorRequired] is true, [token]/[refreshToken] are null and
 * [twoFactorToken] carries the short-lived challenge to complete at /api/auth/2fa/verify.
 */
data class AuthResponse(
    val token: String?,
    val refreshToken: String?,
    val username: String,
    val email: String,
    val message: String,
    val twoFactorRequired: Boolean = false,
    val twoFactorToken: String? = null
)
