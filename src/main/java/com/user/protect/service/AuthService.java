package com.user.protect.service;

import com.user.protect.dto.LoginDTO;
import com.user.protect.dto.TokenResponseDTO;
import com.user.protect.dto.Verify2FaDTO;
import com.user.protect.entity.RevokedToken;
import com.user.protect.entity.User;
import com.user.protect.repository.UserRepository;
import com.user.protect.repository.RevokedTokenRepository; // Importação necessária
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final RevokedTokenRepository revokedTokenRepository; // Adicionado para injeção via Lombok
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final EmailService emailService;

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCK_TIME_DURATION_MINUTES = 15;

    public void initiateLogin(LoginDTO loginDTO) {
        User user = userRepository.findByEmail(loginDTO.email())
                .orElseThrow(() -> new IllegalArgumentException("Usuário ou senha incorretos"));

        // 1. Verifica se a conta está bloqueada
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

        // 2. Valida a senha
        if (!passwordEncoder.matches(loginDTO.password(), user.getPassword())) {
            updateFailedAttempts(user);
            throw new IllegalArgumentException("Usuário ou senha incorretos");
        }

        // 3. Sucesso na senha: Reseta tentativas e gera 2FA
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
        // Garante que não operamos sobre null se o banco tiver dados antigos
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
        // Remove o prefixo "Bearer " caso ele venha no cabeçalho
        String cleanToken = token.replace("Bearer ", "");

        RevokedToken revokedToken = new RevokedToken();
        revokedToken.setToken(cleanToken);

        // Define a expiração da entrada na blacklist.
        // Como o seu JWT dura 15 min, após esse tempo o registro pode ser deletado do banco por um robô de limpeza.
        revokedToken.setExpirationDate(LocalDateTime.now().plusMinutes(15));

        revokedTokenRepository.save(revokedToken);
    }
}