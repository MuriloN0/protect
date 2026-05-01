package com.user.protect.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.user.protect.entity.User;
import com.user.protect.mock.UserMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

class TokenServiceTest {

    private TokenService tokenService;
    private final String SECRET_MOCK = "meu-segredo-super-seguro-mock-para-testes";

    @BeforeEach
    void setUp() {
        tokenService = new TokenService();

        // Injeta o valor da secret simulando o comportamento do @Value
        ReflectionTestUtils.setField(tokenService, "secret", SECRET_MOCK);
    }

    @Test
    @DisplayName("Deve gerar um token JWT válido contendo o email do usuário")
    void generateToken_Success() {
        // Arrange
        User user = UserMock.createValidUser();

        // Act
        String token = tokenService.generateToken(user);

        // Assert
        assertNotNull(token);
        assertFalse(token.isBlank());

        String subject = JWT.decode(token).getSubject();
        String issuer = JWT.decode(token).getIssuer();

        assertEquals(user.getEmail(), subject);
        assertEquals("protect-api", issuer);
        assertTrue(JWT.decode(token).getExpiresAtAsInstant().isAfter(Instant.now()));
    }

    @Test
    @DisplayName("Deve validar um token JWT genuíno e retornar o email (subject)")
    void validateToken_Success() {
        // Arrange
        User user = UserMock.createValidUser();
        String validToken = tokenService.generateToken(user);

        // Act
        String subject = tokenService.validateToken(validToken);

        // Assert
        assertEquals(user.getEmail(), subject);
    }

    @Test
    @DisplayName("Deve retornar uma string vazia ao tentar validar um token alterado ou malformado")
    void validateToken_InvalidToken_ReturnsEmptyString() {
        // Arrange
        String invalidToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.payload_alterado.assinatura_invalida";

        // Act
        String subject = tokenService.validateToken(invalidToken);

        // Assert
        assertEquals("", subject);
    }

    @Test
    @DisplayName("Deve retornar uma string vazia ao tentar validar um token expirado")
    void validateToken_ExpiredToken_ReturnsEmptyString() {
        // Arrange
        Algorithm algorithm = Algorithm.HMAC256(SECRET_MOCK);
        String expiredToken = JWT.create()
                .withIssuer("protect-api")
                .withSubject("aluno@teste.com")
                .withExpiresAt(Instant.now().minus(1, ChronoUnit.DAYS))
                .sign(algorithm);

        // Act
        String subject = tokenService.validateToken(expiredToken);

        // Assert
        assertEquals("", subject);
    }
}