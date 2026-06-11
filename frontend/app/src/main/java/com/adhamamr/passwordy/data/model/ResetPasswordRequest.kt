package com.adhamamr.passwordy.data.model

/** Request body for completing a password reset: the emailed token + the new master password. */
data class ResetPasswordRequest(val token: String, val newPassword: String)
