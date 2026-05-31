package com.adhamamr.passwordy.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

/**
 * A registered account. Stores only the BCrypt {@code masterPasswordHash}, never the master
 * password itself. {@code createdAt} is set on insert and {@code updatedAt} only on update,
 * so a never-edited row has a null {@code updatedAt}.
 */
@Getter
@Setter
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", unique = true, nullable = false)
    private String username;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "master_password_hash", nullable = false)
    private String masterPasswordHash;  // BCrypt hashed password

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public User() {}

    public User(String username, String email, String masterPasswordHash) {
        this.username = username;
        this.email = email;
        this.masterPasswordHash = masterPasswordHash;
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