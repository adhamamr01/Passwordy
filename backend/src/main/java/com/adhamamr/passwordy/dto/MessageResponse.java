package com.adhamamr.passwordy.dto;

/**
 * A bare human-readable status message, used where a response carries no data — e.g. the
 * deliberately generic registration acknowledgement and the verification confirmation.
 */
public record MessageResponse(String message) {
}
