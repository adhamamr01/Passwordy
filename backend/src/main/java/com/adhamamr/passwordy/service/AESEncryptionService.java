package com.adhamamr.passwordy.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
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
 * <p>The 256-bit key comes from {@code encryption.secret.key} (Base64-encoded). The committed
 * profile ships a throwaway dev default; real deployments override it via the gitignored
 * {@code application-{local,docker}.properties}. Rotating the key makes any data encrypted
 * under the old key undecryptable — there is no in-place re-encryption.
 */
@Service
public class AESEncryptionService implements EncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int TAG_LENGTH_BIT = 128;
    private static final int IV_LENGTH_BYTE = 12;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private final SecretKey secretKey;

    public AESEncryptionService(@Value("${encryption.secret.key}") String base64Key) {
        this.secretKey = new SecretKeySpec(Base64.getDecoder().decode(base64Key.trim()), "AES");
    }

    @Override
    public String encrypt(String plainText) throws Exception {
        byte[] iv = new byte[IV_LENGTH_BYTE];
        SECURE_RANDOM.nextBytes(iv);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BIT, iv));
        byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

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

        return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
    }
}
