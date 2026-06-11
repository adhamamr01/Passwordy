package com.adhamamr.passwordy.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

/**
 * A registered account. Stores only the Argon2id {@code masterPasswordHash}, never the master
 * password itself. {@code createdAt} is set on insert; {@code updatedAt} is initialised to
 * {@code createdAt} and refreshed on every update.
 */
@Getter
@Setter
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String masterPasswordHash;

    /** False until the user verifies their email; login is refused until this is true. */
    @Column(nullable = false)
    private boolean enabled = false;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    public User() {}

    public User(String username, String email, String masterPasswordHash) {
        this.username = username;
        this.email = email;
        this.masterPasswordHash = masterPasswordHash;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
