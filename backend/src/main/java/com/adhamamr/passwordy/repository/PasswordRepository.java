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

    /**
     * Returns the user's favorite passwords only, filtered in the database (owner + favorite
     * flag) so favorites are never selected across another user's rows.
     */
    List<Password> findByUserUsernameAndFavoriteTrue(String username);
}
