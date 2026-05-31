package com.adhamamr.passwordy.service;

/**
 * Symmetric encryption for password values at rest. {@code decrypt(encrypt(x))} must equal
 * {@code x}; the encoded form returned by {@link #encrypt} is opaque to callers and is what
 * gets persisted. See {@link AESEncryptionService} for the concrete AES-GCM scheme.
 */
public interface EncryptionService {
    String encrypt(String plainText) throws Exception;
    String decrypt(String encryptedText) throws Exception;
}