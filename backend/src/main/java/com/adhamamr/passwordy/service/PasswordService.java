package com.adhamamr.passwordy.service;

import com.adhamamr.passwordy.dto.PasswordSaveRequest;
import com.adhamamr.passwordy.dto.PasswordResponse;
import java.util.List;

public interface PasswordService {
    String generatePassword(int length, boolean includeSymbols);
    String generatePin(int length);

    PasswordResponse savePassword(PasswordSaveRequest request, String username);
    List<PasswordResponse> getAllPasswords(String username);
    PasswordResponse getPasswordById(Long id, String username);
    PasswordResponse updatePassword(Long id, PasswordSaveRequest request, String username);
    void deletePassword(Long id, String username);
    String decryptPassword(Long id, String username);
}
