package com.adhamamr.passwordy.dto;

import java.util.List;

/**
 * Returned when 2FA is enabled: the one-time recovery codes, shown <b>once</b> (only their
 * hashes are stored). The user must save them to regain access if they lose their authenticator.
 */
public record TotpEnableResponse(List<String> recoveryCodes, String message) {
}
