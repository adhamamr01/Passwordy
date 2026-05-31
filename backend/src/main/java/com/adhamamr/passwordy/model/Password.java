package com.adhamamr.passwordy.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

/**
 * A stored credential owned by a {@link User}. The {@code value} column holds the
 * AES-GCM-encrypted password (never plaintext). {@code createdAt} is set on insert and
 * {@code updatedAt} only on update, so a never-edited row has a null {@code updatedAt}.
 */
@Getter
@Setter
@Entity
@Table(name = "passwords")
public class Password {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "label", nullable = false)
    private String label;

    @Column(name = "password_value", nullable = false)
    private String value;

    @Column(name = "username")
    private String username;

    @Column(name = "url")
    private String url;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "category")
    private String category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Password() {}

    public Password(String value) {
        this.value = value;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}