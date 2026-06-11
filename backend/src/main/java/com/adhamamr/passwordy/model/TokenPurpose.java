package com.adhamamr.passwordy.model;

/** What a {@link VerificationToken} authorises: confirming an email, or resetting a password. */
public enum TokenPurpose {
    VERIFY_EMAIL,
    PASSWORD_RESET
}
