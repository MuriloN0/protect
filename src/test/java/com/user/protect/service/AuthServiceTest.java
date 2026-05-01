package com.user.protect.service;

import com.user.protect.dto.LoginDTO;
import com.user.protect.dto.TokenResponseDTO;
import com.user.protect.dto.Verify2FaDTO;
import com.user.protect.entity.RevokedToken;
import com.user.protect.entity.User;
import com.user.protect.mock.UserMock;
import com.user.protect.repository.RevokedTokenRepository;
import com.user.protect.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RevokedTokenRepository revokedTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    @Captor
    private ArgumentCaptor<RevokedToken> revokedTokenCaptor;

    @Test
    @DisplayName("Deve iniciar login com sucesso e enviar código 2FA")
    void initiateLogin_Success() {
        // Arrange
        User validUser = UserMock.createValidUser();
        LoginDTO loginDTO = new LoginDTO(validUser.getEmail(), "senhaCorreta"); // Assumindo que LoginDTO é um Record ou tem construtor

        when(userRepository.findByEmail(loginDTO.email())).thenReturn(Optional.of(validUser));
        when(passwordEncoder.matches(loginDTO.password(), validUser.getPassword())).thenReturn(true);

        // Act
        authService.initiateLogin(loginDTO);

        // Assert
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertNotNull(savedUser.getTwoFactorCode());
        assertNotNull(savedUser.getTwoFactorExpiresAt());
        assertEquals(0, savedUser.getFailedAttempts());
        verify(emailService).send2FaCode(eq(validUser.getEmail()), anyString());
    }

    @Test
    @DisplayName("Deve lançar exceção quando a senha estiver incorreta e incrementar tentativas falhas")
    void initiateLogin_InvalidPassword_ShouldIncrementAttempts() {
        // Arrange
        User validUser = UserMock.createValidUser();
        LoginDTO loginDTO = new LoginDTO(validUser.getEmail(), "senhaIncorreta");

        when(userRepository.findByEmail(loginDTO.email())).thenReturn(Optional.of(validUser));
        when(passwordEncoder.matches(loginDTO.password(), validUser.getPassword())).thenReturn(false);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> authService.initiateLogin(loginDTO));

        assertEquals("Usuário ou senha incorretos", exception.getMessage());

        verify(userRepository).save(userCaptor.capture());
        assertEquals(1, userCaptor.getValue().getFailedAttempts());
    }

    @Test
    @DisplayName("Deve bloquear a conta após atingir o limite máximo de tentativas falhas")
    void initiateLogin_InvalidPassword_ShouldLockAccount() {
        // Arrange
        User almostLockedUser = UserMock.createValidUser();
        almostLockedUser.setFailedAttempts(4); // Vai para 5 no teste (MAX_FAILED_ATTEMPTS)
        LoginDTO loginDTO = new LoginDTO(almostLockedUser.getEmail(), "senhaIncorreta");

        when(userRepository.findByEmail(loginDTO.email())).thenReturn(Optional.of(almostLockedUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> authService.initiateLogin(loginDTO));

        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertFalse(savedUser.getAccountNonLocked());
        assertNotNull(savedUser.getLockTime());
        assertEquals(5, savedUser.getFailedAttempts());
    }

    // --- TESTES PARA VERIFY 2FA ---

    @Test
    @DisplayName("Deve validar o código 2FA e retornar o token JWT")
    void verify2Fa_Success() {
        // Arrange
        User pending2FaUser = UserMock.createPendingTwoFactorUser();
        Verify2FaDTO dto = new Verify2FaDTO(pending2FaUser.getEmail(), "123456");
        String expectedJwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";

        when(userRepository.findByEmail(dto.email())).thenReturn(Optional.of(pending2FaUser));
        when(tokenService.generateToken(pending2FaUser)).thenReturn(expectedJwt);

        // Act
        TokenResponseDTO response = authService.verify2Fa(dto);

        // Assert
        assertNotNull(response);
        assertEquals(expectedJwt, response.token());

        verify(userRepository).save(userCaptor.capture());
        assertNull(userCaptor.getValue().getTwoFactorCode()); // Garante que limpou o código após o uso
        assertNull(userCaptor.getValue().getTwoFactorExpiresAt());
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar validar um código 2FA expirado")
    void verify2Fa_ExpiredCode() {
        // Arrange
        User expired2FaUser = UserMock.createPendingTwoFactorUser();
        expired2FaUser.setTwoFactorExpiresAt(LocalDateTime.now().minusMinutes(1)); // Expirou há 1 minuto
        Verify2FaDTO dto = new Verify2FaDTO(expired2FaUser.getEmail(), "123456");

        when(userRepository.findByEmail(dto.email())).thenReturn(Optional.of(expired2FaUser));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> authService.verify2Fa(dto));

        assertEquals("Código 2FA expirado", exception.getMessage());
        verify(tokenService, never()).generateToken(any());
    }

    // --- TESTES PARA LOGOUT ---

    @Test
    @DisplayName("Deve revogar o token removendo o prefixo Bearer")
    void logout_Success() {
        // Arrange
        String rawToken = "Bearer meu.token.jwt";

        // Act
        authService.logout(rawToken);

        // Assert
        verify(revokedTokenRepository).save(revokedTokenCaptor.capture());
        RevokedToken savedRevokedToken = revokedTokenCaptor.getValue();

        assertEquals("meu.token.jwt", savedRevokedToken.getToken()); // Verifica se removeu o "Bearer "
        assertTrue(savedRevokedToken.getExpirationDate().isAfter(LocalDateTime.now()));
    }

    // --- TESTES PARA REDEFINIÇÃO DE SENHA ---

    @Test
    @DisplayName("Deve gerar token de redefinição de senha e enviar por e-mail")
    void requestPasswordReset_Success() {
        // Arrange
        User validUser = UserMock.createValidUser();
        when(userRepository.findByEmail(validUser.getEmail())).thenReturn(Optional.of(validUser));

        // Act
        authService.requestPasswordReset(validUser.getEmail());

        // Assert
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertNotNull(savedUser.getResetPasswordToken());
        assertNotNull(savedUser.getResetPasswordTokenExpiration());
        verify(emailService).sendPasswordResetEmail(eq(validUser.getEmail()), anyString());
    }

    @Test
    @DisplayName("Deve redefinir a senha com sucesso, limpar tokens e desbloquear conta")
    void resetPassword_Success() {
        // Arrange
        User resetUser = UserMock.createPasswordResetUser();
        resetUser.setAccountNonLocked(false); // Simulando que a conta estava bloqueada
        resetUser.setFailedAttempts(5);

        String validToken = resetUser.getResetPasswordToken();
        String newPassword = "novaSenha123";

        when(userRepository.findByResetPasswordToken(validToken)).thenReturn(Optional.of(resetUser));
        when(passwordEncoder.encode(newPassword)).thenReturn("novaSenhaHasheada");

        // Act
        authService.resetPassword(validToken, newPassword);

        // Assert
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertEquals("novaSenhaHasheada", savedUser.getPassword());
        assertNull(savedUser.getResetPasswordToken());
        assertNull(savedUser.getResetPasswordTokenExpiration());
        assertTrue(savedUser.getAccountNonLocked()); // Garante que a conta foi desbloqueada
        assertEquals(0, savedUser.getFailedAttempts());
    }
}