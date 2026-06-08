package com.adhamamr.passwordy.repository;

import com.adhamamr.passwordy.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/** Data access for {@link User} entities, with lookups and existence checks by username/email. */
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}