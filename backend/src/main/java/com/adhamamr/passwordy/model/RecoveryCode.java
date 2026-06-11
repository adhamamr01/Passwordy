package com.adhamamr.passwordy.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * A single-use 2FA backup code. Only the SHA-256 {@code codeHash} is stored. Generated as a set
 * when TOTP is enabled; one is consumed (deleted) when used in place of an authenticator code.
 */
@Getter
@Setter
@Entity
@Table(name = "recovery_codes")
public class RecoveryCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String codeHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public RecoveryCode() {}

    public RecoveryCode(String codeHash, User user) {
        this.codeHash = codeHash;
        this.user = user;
    }
}
