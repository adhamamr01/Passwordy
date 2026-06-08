package com.adhamamr.passwordy.repository;

import com.adhamamr.passwordy.model.Password;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Data access for {@link Password} entities. */
public interface PasswordRepository extends JpaRepository<Password, Long> {

    /**
     * Returns all passwords owned by the given user, filtered in the database (via the
     * {@code Password.user.username} path) rather than loading every row and filtering in
     * memory.
     */
    List<Password> findByUserUsername(String username);
}
