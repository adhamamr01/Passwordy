package com.adhamamr.passwordy.data.model

/** Login step 2: the challenge token from step 1 + a TOTP or recovery code. */
data class TwoFactorVerifyRequest(val twoFactorToken: String, val code: String)

/** A 6-digit TOTP code, for enabling/disabling 2FA. */
data class TotpCodeRequest(val code: String)

/** Setup response: the base32 [secret] (manual entry) and [otpauthUri] (QR). */
data class TotpSetupResponse(val secret: String, val otpauthUri: String)

/** Enable response: the one-time recovery codes (shown once) + a status message. */
data class TotpEnableResponse(val recoveryCodes: List<String>?, val message: String)
