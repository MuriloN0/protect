package com.user.protect.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponseDTO(
        UUID id,
        String email,
        boolean mfaEnable,
        LocalDateTime createdAt
) {
}
