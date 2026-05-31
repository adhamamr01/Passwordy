package com.adhamamr.passwordy.service;

import com.adhamamr.passwordy.dto.PasswordSaveRequest;
import com.adhamamr.passwordy.dto.PasswordResponse;
import java.util.List;

/**
 * Password generation and per-user password operations.
 *
 * <p>Every CRUD/decrypt method takes the authenticated {@code username} and operates only on
 * records owned by that user; callers (controllers) supply the username from the security
 * context. {@code value}s returned by read methods are encrypted — use
 * {@link #decryptPassword} to obtain plaintext.
 */
public interface PasswordService {
    String generatePassword(int length, boolean includeSymbols);
    String generatePin(int length);

    PasswordResponse savePassword(PasswordSaveRequest request, String username);
    List<PasswordResponse> getAllPasswords(String username);
    PasswordResponse getPasswordById(Long id, String username);
    PasswordResponse updatePassword(Long id, PasswordSaveRequest request, String username);
    void deletePassword(Long id, String username);

    /** Returns the decrypted plaintext for the user's password, after an ownership check. */
    String decryptPassword(Long id, String username);
}
