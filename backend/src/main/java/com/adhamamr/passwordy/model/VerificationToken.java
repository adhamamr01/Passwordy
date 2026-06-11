package com.adhamamr.passwordy.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * A single-use token tied to a {@link User}, carrying a {@link TokenPurpose} (email
 * verification or password reset). Consumed by the matching {@code /api/auth/*} route; expired,
 * unknown, or wrong-purpose tokens are rejected.
 */
@Getter
@Setter
@Entity
@Table(name = "verification_tokens")
public class VerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TokenPurpose purpose;

    @Column(nullable = false)
    private Instant expiresAt;

    public VerificationToken() {}

    public VerificationToken(String token, User user, TokenPurpose purpose, Instant expiresAt) {
        this.token = token;
        this.user = user;
        this.purpose = purpose;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
