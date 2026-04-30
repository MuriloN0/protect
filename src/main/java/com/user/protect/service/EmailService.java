package com.user.protect.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    @Autowired
    private JavaMailSender mailSender;

    public void send2FaCode(String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Seu código de Autenticação (2FA)");
        message.setText("Seu código de verificação é: " + code + "\nEste código expira em 5 minutos.");

        mailSender.send(message);
    }

    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Recuperação de Senha - Sistema Protect");

        message.setText("Você solicitou a recuperação de senha.\n" +
                "Clique no link abaixo para redefinir sua senha (válido por 15 minutos):\n" +
                resetLink);

        mailSender.send(message);
    }
}