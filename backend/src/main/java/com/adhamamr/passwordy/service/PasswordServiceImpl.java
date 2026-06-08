package com.adhamamr.passwordy.service;

import com.adhamamr.passwordy.dto.PasswordSaveRequest;
import com.adhamamr.passwordy.dto.PasswordResponse;
import com.adhamamr.passwordy.model.Password;
import com.adhamamr.passwordy.model.User;
import com.adhamamr.passwordy.exception.ResourceNotFoundException;
import com.adhamamr.passwordy.exception.UnauthorizedException;
import com.adhamamr.passwordy.repository.PasswordRepository;
import com.adhamamr.passwordy.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Core password business logic: generation, encrypted CRUD, and ownership enforcement.
 *
 * <p>Stored password values are encrypted via {@link EncryptionService} on write and only
 * decrypted on explicit request. Every entry lookup goes through {@link #findOwnedPassword},
 * which is the single authorization gate ensuring a user can only touch their own records.
 */
@Service
public class PasswordServiceImpl implements PasswordService {

    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String NUMBERS = "0123456789";
    private static final String SYMBOLS = "!@#$%^&*()_+";

    private static final SecureRandom random = new SecureRandom();

    private final PasswordRepository passwordRepository;
    private final UserRepository userRepository;
    private final EncryptionService encryptionService;

    public PasswordServiceImpl(PasswordRepository passwordRepository,
                               UserRepository userRepository,
                               EncryptionService encryptionService) {
        this.passwordRepository = passwordRepository;
        this.userRepository = userRepository;
        this.encryptionService = encryptionService;
    }

    /**
     * Generates a random password that always contains at least one uppercase, lowercase,
     * and digit character (plus one symbol when {@code includeSymbols} is true); the
     * remaining characters are drawn from the selected alphabet and the whole string is
     * shuffled so the guaranteed characters are not in fixed positions.
     *
     * @param length total length, must be at least 8
     * @throws IllegalArgumentException if {@code length < 8}
     */
    @Override
    public String generatePassword(int length, boolean includeSymbols) {
        if (length < 8) {
            throw new IllegalArgumentException("Password length must be at least 8 characters");
        }

        StringBuilder chars = new StringBuilder(UPPERCASE + LOWERCASE + NUMBERS);
        StringBuilder password = new StringBuilder();

        password.append(UPPERCASE.charAt(random.nextInt(UPPERCASE.length())));
        password.append(LOWERCASE.charAt(random.nextInt(LOWERCASE.length())));
        password.append(NUMBERS.charAt(random.nextInt(NUMBERS.length())));

        if (includeSymbols) {
            chars.append(SYMBOLS);
            password.append(SYMBOLS.charAt(random.nextInt(SYMBOLS.length())));
        }

        for (int i = password.length(); i < length; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }

        return shuffleString(password.toString());
    }

    private String shuffleString(String input) {
        char[] chars = input.toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }
        return new String(chars);
    }

    @Override
    public PasswordResponse savePassword(PasswordSaveRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Password password = new Password();
        password.setLabel(request.label());
        password.setValue(encryptSafe(request.password()));
        password.setUsername(request.username());
        password.setUrl(request.url());
        password.setNotes(request.notes());
        password.setCategory(request.category());
        password.setUser(user);

        return toResponse(passwordRepository.save(password));
    }

    @Override
    public List<PasswordResponse> getAllPasswords(String username) {
        return passwordRepository.findByUserUsername(username).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PasswordResponse getPasswordById(Long id, String username) {
        return toResponse(findOwnedPassword(id, username));
    }

    @Override
    public PasswordResponse updatePassword(Long id, PasswordSaveRequest request, String username) {
        Password password = findOwnedPassword(id, username);

        password.setLabel(request.label());
        password.setValue(encryptSafe(request.password()));
        password.setUsername(request.username());
        password.setUrl(request.url());
        password.setNotes(request.notes());
        password.setCategory(request.category());

        return toResponse(passwordRepository.save(password));
    }

    @Override
    public void deletePassword(Long id, String username) {
        findOwnedPassword(id, username);
        passwordRepository.deleteById(id);
    }

    @Override
    public String decryptPassword(Long id, String username) {
        Password password = findOwnedPassword(id, username);
        try {
            return encryptionService.decrypt(password.getValue());
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt password", e);
        }
    }

    @Override
    public String generatePin(int length) {
        if (length < 4) {
            throw new IllegalArgumentException("PIN length must be at least 4 digits");
        }
        if (length > 12) {
            throw new IllegalArgumentException("PIN length cannot exceed 12 digits");
        }

        StringBuilder pin = new StringBuilder();
        for (int i = 0; i < length; i++) {
            pin.append(NUMBERS.charAt(random.nextInt(NUMBERS.length())));
        }
        return pin.toString();
    }

    /**
     * Loads a password and verifies it belongs to {@code username}. This is the single
     * authorization gate shared by get/update/delete/decrypt.
     *
     * @throws ResourceNotFoundException if no password has the given id (→ HTTP 404)
     * @throws UnauthorizedException if the password belongs to another user (→ HTTP 403)
     */
    private Password findOwnedPassword(Long id, String username) {
        Password password = passwordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Password not found with id: " + id));
        if (!password.getUser().getUsername().equals(username)) {
            throw new UnauthorizedException("Unauthorized access to password");
        }
        return password;
    }

    private String encryptSafe(String plainText) {
        try {
            return encryptionService.encrypt(plainText);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt password", e);
        }
    }

    private PasswordResponse toResponse(Password password) {
        return new PasswordResponse(
                password.getId(),
                password.getLabel(),
                password.getValue(),
                password.getUsername(),
                password.getUrl(),
                password.getNotes(),
                password.getCategory(),
                password.getCreatedAt(),
                password.getUpdatedAt()
        );
    }
}
