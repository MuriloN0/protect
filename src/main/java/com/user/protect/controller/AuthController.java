package com.user.protect.controller;

import com.user.protect.dto.LoginDTO;
import com.user.protect.dto.TokenResponseDTO;
import com.user.protect.dto.Verify2FaDTO;
import com.user.protect.service.AuthService;
import jakarta.servlet.http.HttpServletRequest; // Importante para recuperar o Header
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<String> login(@Valid @RequestBody LoginDTO dto) {
        authService.initiateLogin(dto);
        return ResponseEntity.ok("Credenciais validadas. Verifique seu e-mail para inserir o código 2FA.");
    }

    @PostMapping("/verify-2fa")
    public ResponseEntity<TokenResponseDTO> verify2Fa(@Valid @RequestBody Verify2FaDTO dto) {
        TokenResponseDTO token = authService.verify2Fa(dto);
        return ResponseEntity.ok(token);
    }

    /**
     * Endpoint para invalidar a sessão (Logout)
     * Atende ao Requisito 1.10 do PI
     */
    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request) {
        String token = this.recoverToken(request);
        if (token != null) {
            authService.logout(token);
        }
        return ResponseEntity.ok("Logout realizado com sucesso. Sessão invalidada no servidor.");
    }

    // Método auxiliar para extrair o token do cabeçalho Authorization
    private String recoverToken(HttpServletRequest request) {
        var authHeader = request.getHeader("Authorization");
        if (authHeader == null) return null;
        return authHeader.replace("Bearer ", "");
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@Valid @RequestBody ForgotPasswordDTO request) {
        // Sempre retorna a mesma mensagem para evitar enumeração de usuários (OWASP)
        authService.requestPasswordReset(request.email());
        return ResponseEntity.ok("Se o e-mail estiver cadastrado, você receberá um link de recuperação em breve.");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordDTO request) {
        try {
            authService.resetPassword(request.token(), request.newPassword());
            return ResponseEntity.ok("Senha redefinida com sucesso.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}