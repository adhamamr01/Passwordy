package com.adhamamr.passwordy.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AESEncryptionServiceTest {

    // Base64-encoded 32-byte (256-bit) test key.
    private static final String TEST_KEY = "pIi2Yod1hDyFQiXIJr5MuR6L0LXuRPSNnISn6W5YdSM=";

    private AESEncryptionService service;

    @BeforeEach
    void setUp() {
        service = new AESEncryptionService(TEST_KEY);
    }

    @Test
    void encryptThenDecrypt_returnsOriginal() throws Exception {
        String plaintext = "my-secret-password";
        assertThat(service.decrypt(service.encrypt(plaintext))).isEqualTo(plaintext);
    }

    @Test
    void encrypt_producesDifferentCiphertextEachTime() throws Exception {
        String plaintext = "same-input";
        assertThat(service.encrypt(plaintext)).isNotEqualTo(service.encrypt(plaintext));
    }

    @Test
    void decrypt_tamperedCiphertext_throws() {
        assertThatThrownBy(() -> service.decrypt("notvalidbase64=="));
    }

    @Test
    void encryptDecrypt_unicodeInput_roundtrips() throws Exception {
        String unicode = "pässwörد123!";
        assertThat(service.decrypt(service.encrypt(unicode))).isEqualTo(unicode);
    }
}
