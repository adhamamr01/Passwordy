package com.adhamamr.passwordy.repository;

import com.adhamamr.passwordy.model.User;
import com.adhamamr.passwordy.model.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Data access for {@link VerificationToken} entities. */
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
    Optional<VerificationToken> findByToken(String token);

    /** Deletes every outstanding token for the given user (used when deleting the account). */
    void deleteByUser(User user);
}
