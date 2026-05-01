package com.user.protect.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @Captor
    private ArgumentCaptor<SimpleMailMessage> messageCaptor;

    @Test
    @DisplayName("Deve enviar e-mail de 2FA com destinatário, assunto e código corretos")
    void send2FaCode_ShouldSendEmailWithCorrectDetails() {
        // Arrange
        String toEmail = "aluno@faculdade.com.br";
        String code = "987654";

        // Act
        emailService.send2FaCode(toEmail, code);

        // Assert
        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage capturedMessage = messageCaptor.getValue();

        assertNotNull(capturedMessage);
        assertArrayEquals(new String[]{toEmail}, capturedMessage.getTo());
        assertEquals("Seu código de Autenticação (2FA)", capturedMessage.getSubject());
        assertTrue(Objects.requireNonNull(capturedMessage.getText()).contains(code));
        assertTrue(capturedMessage.getText().contains("expira em 5 minutos"));
    }

    @Test
    @DisplayName("Deve enviar e-mail de recuperação de senha com destinatário, assunto e link corretos")
    void sendPasswordResetEmail_ShouldSendEmailWithCorrectDetails() {
        // Arrange
        String toEmail = "esqueci.senha@faculdade.com.br";
        String resetLink = "http://localhost:4200/reset-password?token=uuid-mockado-123";

        // Act
        emailService.sendPasswordResetEmail(toEmail, resetLink);

        // Assert
        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage capturedMessage = messageCaptor.getValue();

        assertNotNull(capturedMessage);
        assertArrayEquals(new String[]{toEmail}, capturedMessage.getTo());
        assertEquals("Recuperação de Senha - Sistema Protect", capturedMessage.getSubject());
        assertTrue(Objects.requireNonNull(capturedMessage.getText()).contains(resetLink));
        assertTrue(capturedMessage.getText().contains("válido por 15 minutos"));
    }
}