package com.adhamamr.passwordy.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

/**
 * A stored credential owned by a {@link User}. The {@code value} column holds the
 * AES-GCM-encrypted password (never plaintext). {@code createdAt} is set on insert;
 * {@code updatedAt} is initialised to {@code createdAt} and refreshed on every update.
 */
@Getter
@Setter
@Entity
@Table(name = "passwords")
public class Password {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String label;

    @Column(name = "password_value", nullable = false)
    private String value;

    private String username;

    private String url;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private String category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    public Password() {}

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
