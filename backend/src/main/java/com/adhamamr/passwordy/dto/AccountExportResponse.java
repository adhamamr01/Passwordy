package com.adhamamr.passwordy.dto;

import java.time.Instant;
import java.util.List;

/**
 * A complete, machine-readable export of everything the service holds about a user (GDPR data
 * portability). Vault entries are returned <b>decrypted</b> so the export is actually usable for
 * migration — it is only ever produced for the authenticated owner over the (TLS) API.
 */
public record AccountExportResponse(
        ExportedAccount account,
        List<ExportedPassword> passwords,
        Instant exportedAt
) {
    /** The account's own profile fields (no password hash is ever exported). */
    public record ExportedAccount(
            String username,
            String email,
            boolean twoFactorEnabled,
            Instant createdAt
    ) {}

    /** One vault entry with its decrypted secret. */
    public record ExportedPassword(
            String label,
            String username,
            String password,
            String url,
            String notes,
            String category,
            boolean favorite,
            Instant createdAt,
            Instant updatedAt
    ) {}
}
