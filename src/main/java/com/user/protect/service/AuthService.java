package com.user.protect.service;

import com.user.protect.dto.LoginDTO;
import com.user.protect.dto.TokenResponseDTO;
import com.user.protect.dto.Verify2FaDTO;
import com.user.protect.entity.RevokedToken;
import com.user.protect.entity.User;
import com.user.protect.repository.UserRepository;
import com.user.protect.repository.RevokedTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RevokedTokenRepository revokedTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final EmailService emailService;

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCK_TIME_DURATION_MINUTES = 15;

    public void initiateLogin(LoginDTO loginDTO) {
        User user = userRepository.findByEmail(loginDTO.email())
                .orElseThrow(() -> new IllegalArgumentException("Usuário ou senha incorretos"));

        if (Boolean.FALSE.equals(user.getAccountNonLocked())) {
            if (user.getLockTime() != null && user.getLockTime().plusMinutes(LOCK_TIME_DURATION_MINUTES).isBefore(LocalDateTime.now())) {
                user.setAccountNonLocked(true);
                user.setFailedAttempts(0);
                user.setLockTime(null);
                userRepository.save(user);
            } else {
                throw new RuntimeException("Conta bloqueada temporariamente devido a múltiplas tentativas falhas. Tente novamente mais tarde.");
            }
        }

        if (!passwordEncoder.matches(loginDTO.password(), user.getPassword())) {
            updateFailedAttempts(user);
            throw new IllegalArgumentException("Usuário ou senha incorretos");
        }

        user.setFailedAttempts(0);

        SecureRandom random = new SecureRandom();
        String code = String.valueOf(100000 + random.nextInt(900000));

        user.setTwoFactorCode(code);
        user.setTwoFactorExpiresAt(LocalDateTime.now().plusMinutes(5));
        userRepository.save(user);

        emailService.send2FaCode(user.getEmail(), code);
    }

    public TokenResponseDTO verify2Fa(Verify2FaDTO dto) {
        User user = userRepository.findByEmail(dto.email())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

        if (user.getTwoFactorCode() == null || !user.getTwoFactorCode().equals(dto.code())) {
            throw new IllegalArgumentException("Código 2FA inválido");
        }

        if (user.getTwoFactorExpiresAt() == null || user.getTwoFactorExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Código 2FA expirado");
        }

        user.setTwoFactorCode(null);
        user.setTwoFactorExpiresAt(null);
        userRepository.save(user);

        String token = tokenService.generateToken(user);
        return new TokenResponseDTO(token);
    }

    private void updateFailedAttempts(User user) {
        int currentAttempts = (user.getFailedAttempts() == null) ? 0 : user.getFailedAttempts();
        int newAttempts = currentAttempts + 1;

        user.setFailedAttempts(newAttempts);

        if (newAttempts >= MAX_FAILED_ATTEMPTS) {
            user.setAccountNonLocked(false);
            user.setLockTime(LocalDateTime.now());
        }

        userRepository.save(user);
    }

    public void logout(String token) {
        String cleanToken = token.replace("Bearer ", "");

        RevokedToken revokedToken = new RevokedToken();
        revokedToken.setToken(cleanToken);
        revokedToken.setExpirationDate(LocalDateTime.now().plusMinutes(15));

        revokedTokenRepository.save(revokedToken);
    }

    public void requestPasswordReset(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            log.warn("AUDIT - Tentativa de recuperação de senha para e-mail inexistente: {}", email);
            return;
        }

        User user = userOpt.get();
        String token = UUID.randomUUID().toString();

        user.setResetPasswordToken(token);
        user.setResetPasswordTokenExpiration(LocalDateTime.now().plusMinutes(15));

        userRepository.save(user);
        emailService.sendPasswordResetEmail(user.getEmail(), token); // Crie este método no EmailService

        log.info("AUDIT - Token de recuperação de senha gerado com sucesso para o usuário: {}", email);
    }

    public void resetPassword(String token, String newPassword) {
        Optional<User> userOpt = userRepository.findByResetPasswordToken(token);

        if (userOpt.isEmpty()) {
            log.error("AUDIT - Falha na redefinição de senha: Token inválido ou não encontrado.");
            throw new IllegalArgumentException("Token inválido ou expirado.");
        }

        User user = userOpt.get();

        if (user.getResetPasswordTokenExpiration().isBefore(LocalDateTime.now())) {
            log.error("AUDIT - Falha na redefinição de senha: Token expirado para o usuário: {}", user.getEmail());
            throw new IllegalArgumentException("Token expirado.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetPasswordToken(null);
        user.setResetPasswordTokenExpiration(null);

        user.setAccountNonLocked(true);
        user.setFailedAttempts(0);
        user.setLockTime(null);

        userRepository.save(user);
        log.info("AUDIT - Senha redefinida com sucesso para o usuário: {}", user.getEmail());
    }
}