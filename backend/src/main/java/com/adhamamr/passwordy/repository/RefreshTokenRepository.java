package com.adhamamr.passwordy.repository;

import com.adhamamr.passwordy.model.RefreshToken;
import com.adhamamr.passwordy.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Data access for {@link RefreshToken} entities. */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    void deleteByUser(User user);
}
