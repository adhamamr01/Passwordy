package com.adhamamr.passwordy.service;

import com.adhamamr.passwordy.dto.PasswordResponse;
import com.adhamamr.passwordy.dto.PasswordSaveRequest;
import com.adhamamr.passwordy.exception.ResourceNotFoundException;
import com.adhamamr.passwordy.exception.UnauthorizedException;
import com.adhamamr.passwordy.model.Password;
import com.adhamamr.passwordy.model.User;
import com.adhamamr.passwordy.repository.PasswordRepository;
import com.adhamamr.passwordy.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordServiceImplTest {

    @Mock PasswordRepository passwordRepository;
    @Mock UserRepository userRepository;
    @Mock EncryptionService encryptionService;

    private PasswordServiceImpl service;

    private User alice;

    @BeforeEach
    void setUp() {
        service = new PasswordServiceImpl(passwordRepository, userRepository, encryptionService);
        alice = new User("alice", "alice@example.com", "$hashed$");
    }

    // --- generatePassword ---

    @Test
    void generatePassword_validLength_returnsCorrectLength() {
        assertThat(service.generatePassword(12, false)).hasSize(12);
    }

    @Test
    void generatePassword_tooShort_throws() {
        assertThatThrownBy(() -> service.generatePassword(4, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void generatePassword_withSymbols_containsSymbol() {
        boolean foundSymbol = false;
        for (int i = 0; i < 20; i++) {
            if (service.generatePassword(16, true).matches(".*[!@#$%^&*()_+].*")) {
                foundSymbol = true;
                break;
            }
        }
        assertThat(foundSymbol).isTrue();
    }

    // --- generatePin ---

    @Test
    void generatePin_validLength_returnsDigitsOnly() {
        assertThat(service.generatePin(6)).matches("\\d{6}");
    }

    @Test
    void generatePin_tooShort_throws() {
        assertThatThrownBy(() -> service.generatePin(3))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void generatePin_tooLong_throws() {
        assertThatThrownBy(() -> service.generatePin(13))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- savePassword ---

    @Test
    void savePassword_validRequest_encryptsAndSaves() throws Exception {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(encryptionService.encrypt("secret")).thenReturn("encrypted");
        Password saved = buildPassword(1L, "encrypted", alice);
        when(passwordRepository.save(any())).thenReturn(saved);

        PasswordResponse response = service.savePassword(
                new PasswordSaveRequest("Gmail", "secret", "alice@gmail.com", "https://gmail.com", null, "Email"),
                "alice");

        assertThat(response.label()).isEqualTo("Gmail");
        assertThat(response.value()).isEqualTo("encrypted");
        verify(encryptionService).encrypt("secret");
    }

    @Test
    void savePassword_unknownUser_throws() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.savePassword(
                new PasswordSaveRequest("x", "y", null, null, null, "Other"), "ghost"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- getAllPasswords ---

    @Test
    void getAllPasswords_returnsOnlyUsersPasswords() {
        Password p = buildPassword(1L, "enc", alice);
        when(passwordRepository.findByUserUsername("alice")).thenReturn(List.of(p));

        List<PasswordResponse> responses = service.getAllPasswords("alice");

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).id()).isEqualTo(1L);
    }

    // --- getPasswordById ---

    @Test
    void getPasswordById_ownPassword_returnsIt() {
        Password p = buildPassword(1L, "enc", alice);
        when(passwordRepository.findById(1L)).thenReturn(Optional.of(p));

        assertThat(service.getPasswordById(1L, "alice").id()).isEqualTo(1L);
    }

    @Test
    void getPasswordById_otherUsersPassword_throwsUnauthorized() {
        User bob = new User("bob", "bob@example.com", "$hashed$");
        Password p = buildPassword(1L, "enc", bob);
        when(passwordRepository.findById(1L)).thenReturn(Optional.of(p));

        assertThatThrownBy(() -> service.getPasswordById(1L, "alice"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void getPasswordById_notFound_throwsResourceNotFound() {
        when(passwordRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPasswordById(99L, "alice"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- deletePassword ---

    @Test
    void deletePassword_ownPassword_deletes() {
        Password p = buildPassword(1L, "enc", alice);
        when(passwordRepository.findById(1L)).thenReturn(Optional.of(p));

        service.deletePassword(1L, "alice");

        verify(passwordRepository).deleteById(1L);
    }

    @Test
    void deletePassword_otherUsersPassword_throwsUnauthorized() {
        User bob = new User("bob", "bob@example.com", "$hashed$");
        Password p = buildPassword(1L, "enc", bob);
        when(passwordRepository.findById(1L)).thenReturn(Optional.of(p));

        assertThatThrownBy(() -> service.deletePassword(1L, "alice"))
                .isInstanceOf(UnauthorizedException.class);
        verify(passwordRepository, never()).deleteById(any());
    }

    // --- decryptPassword ---

    @Test
    void decryptPassword_ownPassword_returnsPlaintext() throws Exception {
        Password p = buildPassword(1L, "encrypted", alice);
        when(passwordRepository.findById(1L)).thenReturn(Optional.of(p));
        when(encryptionService.decrypt("encrypted")).thenReturn("secret");

        assertThat(service.decryptPassword(1L, "alice")).isEqualTo("secret");
    }

    // --- helpers ---

    private Password buildPassword(Long id, String encryptedValue, User owner) {
        Password p = new Password();
        p.setLabel("Gmail");
        p.setValue(encryptedValue);
        p.setCategory("Email");
        p.setUser(owner);
        // reflection to set id since there's no setter
        try {
            var f = Password.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(p, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return p;
    }
}
