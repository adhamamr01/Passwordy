package com.adhamamr.passwordy.service;

import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import org.springframework.stereotype.Service;

/**
 * TOTP (RFC 6238) helper: generates shared secrets, builds the {@code otpauth://} URI the client
 * renders as a QR code, and verifies 6-digit codes (with the library's default ±1 step window).
 */
@Service
public class TotpService {

    private static final String ISSUER = "Passwordy";

    private final SecretGenerator secretGenerator = new DefaultSecretGenerator();
    private final CodeVerifier codeVerifier =
            new DefaultCodeVerifier(new DefaultCodeGenerator(), new SystemTimeProvider());

    public String generateSecret() {
        return secretGenerator.generate();
    }

    /** The {@code otpauth://totp/...} URI to encode as a QR / enter into an authenticator app. */
    public String otpAuthUri(String secret, String username) {
        return new QrData.Builder()
                .label(ISSUER + ":" + username)
                .secret(secret)
                .issuer(ISSUER)
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build()
                .getUri();
    }

    public boolean verify(String secret, String code) {
        return code != null && secret != null && codeVerifier.isValidCode(secret, code);
    }
}
