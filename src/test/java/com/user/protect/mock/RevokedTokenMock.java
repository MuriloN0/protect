package com.user.protect.mock;

import com.user.protect.entity.RevokedToken;
import java.time.LocalDateTime;
import java.util.UUID;

public class RevokedTokenMock {

    public static RevokedToken createValidRevokedToken() {
        return new RevokedToken(
                UUID.randomUUID(),
                "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.generic_mocked_token_string",
                LocalDateTime.now().plusDays(1)
        );
    }

    /**
     * Retorna um token que já passou da data de expiração.
     */
    public static RevokedToken createExpiredRevokedToken() {
        return new RevokedToken(
                UUID.randomUUID(),
                "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.expired_mocked_token_string",
                LocalDateTime.now().minusDays(1) // Data de expiração no passado (ontem)
        );
    }

    /**
     * Permite injetar um token específico (útil para testar injeção de SQL/XSS ou formatos inválidos).
     */
    public static RevokedToken createWithCustomTokenString(String customToken) {
        return new RevokedToken(
                UUID.randomUUID(),
                customToken,
                LocalDateTime.now().plusHours(2)
        );
    }
}