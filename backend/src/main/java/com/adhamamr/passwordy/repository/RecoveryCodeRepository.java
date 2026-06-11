package com.adhamamr.passwordy.repository;

import com.adhamamr.passwordy.model.RecoveryCode;
import com.adhamamr.passwordy.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Data access for {@link RecoveryCode} entities. */
public interface RecoveryCodeRepository extends JpaRepository<RecoveryCode, Long> {
    Optional<RecoveryCode> findByUserAndCodeHash(User user, String codeHash);
    void deleteByUser(User user);
}
