package com.adhamamr.passwordy.service;

import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * AES-256-GCM implementation of {@link EncryptionService} for stored password values.
 *
 * <p>Each {@link #encrypt} call generates a fresh random 12-byte IV — GCM must never reuse
 * an IV under the same key, or confidentiality and authentication both break. The stored
 * format is {@code Base64( IV(12 bytes) || ciphertext+tag )}, so every value is
 * self-contained and {@link #decrypt} needs no external IV bookkeeping. GCM's auth tag
 * also makes tampering with a stored value fail loudly on decrypt.
 *
 * <p>The 256-bit key is hardcoded for development only; production should load it from
 * configuration/secrets (see DECISIONS.md §6).
 */
@Service
public class AESEncryptionService implements EncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int TAG_LENGTH_BIT = 128;
    private static final int IV_LENGTH_BYTE = 12;

    /** Development-only key. Externalize to env/vault for production (DECISIONS.md §6). */
    private static final String SECRET_KEY = "MySecretKey12345MySecretKey12345";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private final javax.crypto.SecretKey secretKey;

    public AESEncryptionService() {
        this.secretKey = new SecretKeySpec(SECRET_KEY.getBytes(), "AES");
    }

    @Override
    public String encrypt(String plainText) throws Exception {
        byte[] iv = new byte[IV_LENGTH_BYTE];
        SECURE_RANDOM.nextBytes(iv);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BIT, iv));
        byte[] encrypted = cipher.doFinal(plainText.getBytes());

        byte[] combined = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    @Override
    public String decrypt(String encryptedText) throws Exception {
        byte[] combined = Base64.getDecoder().decode(encryptedText);

        byte[] iv = Arrays.copyOfRange(combined, 0, IV_LENGTH_BYTE);
        byte[] encrypted = Arrays.copyOfRange(combined, IV_LENGTH_BYTE, combined.length);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BIT, iv));

        return new String(cipher.doFinal(encrypted));
    }
}
