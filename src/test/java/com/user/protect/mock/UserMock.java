package com.user.protect.mock;

import com.user.protect.entity.User;
import java.time.LocalDateTime;
import java.util.UUID;

public class UserMock {

    /**
     * Retorna um usuário padrão, com conta ativa e sem bloqueios.
     */
    public static User createValidUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .email("usuario.valido@teste.com")
                .password("$2a$10$hashDeSenhaMockado123456789")
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Retorna um usuário que excedeu as tentativas de login e está bloqueado.
     */
    public static User createLockedUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .email("usuario.bloqueado@teste.com")
                .password("$2a$10$hashDeSenhaMockado123456789")
                .failedAttempts(5)
                .failedLoginAttempts(5)
                .accountNonLocked(false)
                .lockTime(LocalDateTime.now().minusMinutes(5)) // Bloqueado há 5 minutos
                .accountLockedUntil(LocalDateTime.now().plusMinutes(25)) // Ficará bloqueado por mais 25 minutos
                .createdAt(LocalDateTime.now().minusDays(10))
                .build();
    }

    /**
     * Retorna um usuário com MFA habilitada.
     */
    public static User createMfaEnabledUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .email("usuario.mfa@teste.com")
                .password("$2a$10$hashDeSenhaMockado123456789")
                .mfaEnabled(true)
                .mfaSecret("JBSWY3DPEHPK3PXP") // Exemplo de secret Base32 mockado
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Retorna um usuário no meio do fluxo de recuperação de senha.
     */
    public static User createPasswordResetUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .email("recuperacao@teste.com")
                .password("$2a$10$hashDeSenhaMockado123456789")
                .resetPasswordToken("token-de-recuperacao-mock-9876")
                .resetPasswordTokenExpiration(LocalDateTime.now().plusHours(1)) // Token válido por mais 1 hora
                .createdAt(LocalDateTime.now().minusMonths(1))
                .build();
    }

    /**
     * Retorna um usuário com um código de MFA gerado, aguardando validação.
     */
    public static User createPendingTwoFactorUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .email("aguardando.mfa@teste.com")
                .password("$2a$10$hashDeSenhaMockado123456789")
                .mfaEnabled(false) // Pode ser falso se o 2FA for padrão para todos via email
                .twoFactorCode("123456")
                .twoFactorExpiresAt(LocalDateTime.now().plusMinutes(5))
                .createdAt(LocalDateTime.now())
                .build();
    }
}