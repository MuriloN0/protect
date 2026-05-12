package com.user.protect.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.user.protect.dto.ForgotPasswordDTO;
import com.user.protect.dto.LoginDTO;
import com.user.protect.dto.ResetPasswordDTO;
import com.user.protect.dto.TokenResponseDTO;
import com.user.protect.dto.Verify2FaDTO;
import com.user.protect.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
    }

    @Test
    @DisplayName("Deve retornar 200 OK e mensagem de sucesso ao iniciar login")
    void login_Success() throws Exception {
        // Arrange
        LoginDTO loginDTO = new LoginDTO("usuario@teste.com", "senhaForte123");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDTO)))
                .andExpect(status().isOk())
                .andExpect(content().string("Credenciais validadas. Verifique seu e-mail para inserir o código 2FA."));

        verify(authService).initiateLogin(any(LoginDTO.class));
    }

    @Test
    @DisplayName("Deve retornar 200 OK e o token JWT ao validar 2FA com sucesso")
    void verify2Fa_Success() throws Exception {
        // Arrange
        Verify2FaDTO verifyDTO = new Verify2FaDTO("usuario@teste.com", "123456");
        TokenResponseDTO tokenResponse = new TokenResponseDTO("eyJhbGciOiJIUzI1NiJ9.meu_token_mockado");

        when(authService.verify2Fa(any(Verify2FaDTO.class))).thenReturn(tokenResponse);

        // Act & Assert
        mockMvc.perform(post("/api/auth/verify-2fa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(tokenResponse.token()));
    }

    // --- TESTES PARA /logout ---

    @Test
    @DisplayName("Deve retornar 200 OK e chamar o serviço de logout ao receber um Authorization Header válido")
    void logout_WithToken_Success() throws Exception {
        // Arrange
        String tokenComBearer = "Bearer meutokenjwt123";
        String tokenLimpo = "meutokenjwt123";

        // Act & Assert
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", tokenComBearer))
                .andExpect(status().isOk())
                .andExpect(content().string("Logout realizado com sucesso. Sessão invalidada no servidor."));

        verify(authService).logout(tokenLimpo);
    }

    @Test
    @DisplayName("Deve retornar 200 OK mesmo sem enviar token, sem chamar a exclusão do serviço")
    void logout_WithoutToken_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/auth/logout")) // Nenhum header Authorization enviado
                .andExpect(status().isOk())
                .andExpect(content().string("Logout realizado com sucesso. Sessão invalidada no servidor."));

        verify(authService, never()).logout(anyString());
    }

    // --- TESTES PARA /forgot-password ---

    @Test
    @DisplayName("Deve retornar 200 OK ao solicitar recuperação de senha")
    void forgotPassword_Success() throws Exception {
        // Arrange
        ForgotPasswordDTO forgotDTO = new ForgotPasswordDTO("usuario@teste.com"); // Assumindo que seu DTO tem o email

        // Act & Assert
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(forgotDTO)))
                .andExpect(status().isOk())
                .andExpect(content().string("Se o e-mail estiver cadastrado, você receberá um link de recuperação em breve."));

        verify(authService).requestPasswordReset(forgotDTO.email());
    }

    // --- TESTES PARA /reset-password ---

    @Test
    @DisplayName("Deve retornar 200 OK ao redefinir a senha com sucesso")
    void resetPassword_Success() throws Exception {
        // Arrange
        ResetPasswordDTO resetDTO = new ResetPasswordDTO("token-recuperacao-123", "novaSenhaSegura");

        // Act & Assert
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetDTO)))
                .andExpect(status().isOk())
                .andExpect(content().string("Senha redefinida com sucesso."));

        verify(authService).resetPassword(resetDTO.token(), resetDTO.newPassword());
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request se a Service lançar IllegalArgumentException no reset")
    void resetPassword_InvalidToken_ReturnsBadRequest() throws Exception {
        // Arrange
        ResetPasswordDTO resetDTO = new ResetPasswordDTO("token-invalido", "novaSenhaSegura");

        // Simulando que o service recusou o token
        doThrow(new IllegalArgumentException("Token inválido ou expirado."))
                .when(authService).resetPassword(anyString(), anyString());

        // Act & Assert
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetDTO)))
                .andExpect(status().isBadRequest()) // Aqui o seu bloco catch deve capturar e transformar num 400!
                .andExpect(content().string("Token inválido ou expirado."));
    }
}