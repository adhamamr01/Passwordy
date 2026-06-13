package com.adhamamr.passwordy.data.model

/**
 * Mirrors the backend GDPR export from `GET /api/account/export`. Timestamps arrive as ISO-8601
 * strings (Spring's default Instant serialization); kept as String since the export is written
 * straight back out to a file.
 */
data class AccountExportResponse(
    val account: ExportedAccount,
    val passwords: List<ExportedPassword>,
    val exportedAt: String?
)

data class ExportedAccount(
    val username: String,
    val email: String,
    val twoFactorEnabled: Boolean,
    val createdAt: String?
)

data class ExportedPassword(
    val label: String?,
    val username: String?,
    val password: String?,
    val url: String?,
    val notes: String?,
    val category: String?,
    val favorite: Boolean,
    val createdAt: String?,
    val updatedAt: String?
)
