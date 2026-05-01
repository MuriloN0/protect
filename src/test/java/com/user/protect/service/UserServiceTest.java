package com.user.protect.service;

import com.user.protect.dto.UserCreateDTO;
import com.user.protect.dto.UserResponseDTO;
import com.user.protect.entity.User;
import com.user.protect.mock.UserMock;
import com.user.protect.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    // --- TESTES PARA CREATE USER ---

    @Test
    @DisplayName("Deve criar um usuário com sucesso utilizando dados mockados")
    void createUser_Success() {
        // Arrange
        User mockUser = UserMock.createValidUser();
        UserCreateDTO createDTO = new UserCreateDTO(mockUser.getEmail(), "senhaForte123");

        when(userRepository.existsByEmail(createDTO.email())).thenReturn(false);
        when(passwordEncoder.encode(createDTO.password())).thenReturn(mockUser.getPassword());
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        // Act
        UserResponseDTO response = userService.createUser(createDTO);

        // Assert
        assertNotNull(response);
        assertEquals(mockUser.getId(), response.id());
        assertEquals(mockUser.getEmail(), response.email());
        // Aqui está a correção: chamando mfaEnable() sem o 'd'
        assertEquals(mockUser.getMfaEnabled(), response.mfaEnable());
        assertEquals(mockUser.getCreatedAt(), response.createdAt());

        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar criar usuário com e-mail já existente no mock")
    void createUser_EmailAlreadyExists_ThrowsException() {
        // Arrange
        User mockLockedUser = UserMock.createLockedUser();
        UserCreateDTO createDTO = new UserCreateDTO(mockLockedUser.getEmail(), "outraSenha123");

        when(userRepository.existsByEmail(createDTO.email())).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.createUser(createDTO));

        assertEquals("Email já cadastrado.", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    // --- TESTES PARA GET ALL USERS ---

    @Test
    @DisplayName("Deve retornar uma lista de UserResponseDTO baseada na lista de mocks")
    void getAllUsers_Success() {
        // Arrange
        User validUserMock = UserMock.createValidUser();
        User mfaUserMock = UserMock.createMfaEnabledUser();

        when(userRepository.findAll()).thenReturn(List.of(validUserMock, mfaUserMock));

        // Act
        List<UserResponseDTO> resultList = userService.getAllUsers();

        // Assert
        assertNotNull(resultList);
        assertEquals(2, resultList.size());
        assertEquals(validUserMock.getEmail(), resultList.get(0).email());
        assertEquals(mfaUserMock.getEmail(), resultList.get(1).email());
    }

    // --- TESTES PARA GET USER BY ID ---

    @Test
    @DisplayName("Deve buscar e retornar um usuário específico pelo ID do mock")
    void getUserById_Success() {
        // Arrange
        User mockUser = UserMock.createPasswordResetUser();
        UUID mockId = mockUser.getId();

        when(userRepository.findById(mockId)).thenReturn(Optional.of(mockUser));

        // Act
        UserResponseDTO response = userService.getUserById(mockId);

        // Assert
        assertNotNull(response);
        assertEquals(mockId, response.id());
        assertEquals(mockUser.getEmail(), response.email());
    }

    @Test
    @DisplayName("Deve lançar exceção ao buscar um usuário com ID inexistente")
    void getUserById_NotFound_ThrowsException() {
        // Arrange
        UUID randomId = UUID.randomUUID();
        when(userRepository.findById(randomId)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.getUserById(randomId));

        assertEquals("Usuário não encontrado!", exception.getMessage());
    }

    // --- TESTES PARA DELETE USER ---

    @Test
    @DisplayName("Deve verificar a existência e finalizar sem erros se o usuário existir (deleção pendente)")
    void deleteUser_Success() {
        // Arrange
        User mockUser = UserMock.createValidUser();
        UUID mockId = mockUser.getId();

        when(userRepository.existsById(mockId)).thenReturn(true);

        // Act & Assert
        assertDoesNotThrow(() -> userService.deleteUser(mockId));

        verify(userRepository).existsById(mockId);

        verify(userRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar deletar um usuário inexistente")
    void deleteUser_NotFound_ThrowsException() {
        // Arrange
        UUID randomId = UUID.randomUUID();
        when(userRepository.existsById(randomId)).thenReturn(false);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.deleteUser(randomId));

        assertEquals("Usuário não encontrado!", exception.getMessage());
        verify(userRepository, never()).deleteById(any());
    }
}