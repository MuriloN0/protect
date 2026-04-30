package com.user.protect.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false)
    private String password;

   @Builder.Default
    @Column(name = "mfa_enabled", nullable = false)
    private Boolean mfaEnabled = false;

    @Column(name = "mfa_secret")
    private String mfaSecret;

    @Builder.Default
    @Column(name = "failed_login_attempts", nullable = false)
    private Integer failedLoginAttempts = 0;

    @Column(name = "account_locked_until")
    private LocalDateTime accountLockedUntil;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "two_factor_code")
    private String twoFactorCode;

    @Column(name = "two_factor_expires_at")
    private LocalDateTime twoFactorExpiresAt;

    // TEM valor inicial, ENTÃO USA @Builder.Default
    @Builder.Default
    @Column(name = "failed_attempts", nullable = false)
    private Integer failedAttempts = 0;

    // TEM valor inicial, ENTÃO USA @Builder.Default
    @Builder.Default
    @Column(name = "account_non_locked", nullable = false)
    private Boolean accountNonLocked = true;

    @Column(name = "lock_time")
    private LocalDateTime lockTime;

    @Column(name = "reset_password_token")
    private String resetPasswordToken;

    @Column(name = "reset_password_token_expiration")
    private LocalDateTime resetPasswordTokenExpiration;
}