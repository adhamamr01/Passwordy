package com.adhamamr.passwordy.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AESEncryptionServiceTest {

    private AESEncryptionService service;

    @BeforeEach
    void setUp() {
        service = new AESEncryptionService();
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
