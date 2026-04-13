package com.user.protect.service;

import com.user.protect.dto.UserCreateDTO;
import com.user.protect.dto.UserResponseDTO;
import com.user.protect.entity.User;
import com.user.protect.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponseDTO createUser(UserCreateDTO userCreateDTO) {
        if (userRepository.existsByEmail(userCreateDTO.email())) {
            throw new IllegalArgumentException("Email já cadastrado.");
        }

        User user = User.builder()
                .email(userCreateDTO.email())
                .password(passwordEncoder.encode(userCreateDTO.password()))
                .mfaEnabled((false))
                .failedLoginAttempts(0)
                .build();

        User savedUser = userRepository.save(user);
        return mapToDTO(savedUser);
    }

    //Método auxiliar
    private UserResponseDTO mapToDTO(User user) {
        return new UserResponseDTO(
                user.getId(),
                user.getEmail(),
                user.getMfaEnabled(),
                user.getCreatedAt()
        );
    }

    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToDTO)
                .toList();
    }

    public UserResponseDTO getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado!"));
        return mapToDTO(user);
    }

    @Transactional
    public void deleteUser(UUID id) {
        if(!userRepository.existsById(id)) {
            throw new IllegalArgumentException("Usuário não encontrado!");
        }
    }
}
