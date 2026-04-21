package com.user.protect.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordDTO(
        @NotBlank String token,
        @NotBlank @Size(min = 8, message = "A senha deve ter no mínimo 8 caracteres") String newPassword
) {
}
