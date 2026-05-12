package com.user.protect.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.user.protect.dto.UserCreateDTO;
import com.user.protect.dto.UserResponseDTO;
import com.user.protect.service.UserService;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
    }

    // --- TESTES PARA POST /add ---

    @Test
    @DisplayName("Deve retornar 200 OK e o DTO do usuário ao criar com sucesso")
    void createUser_Success() throws Exception {
        // Arrange
        UserCreateDTO createDTO = new UserCreateDTO("aluno@teste.com", "senhaForte123");
        UUID expectedId = UUID.randomUUID();
        UserResponseDTO responseDTO = new UserResponseDTO(expectedId, createDTO.email(), false, LocalDateTime.now());

        when(userService.createUser(any(UserCreateDTO.class))).thenReturn(responseDTO);

        // Act & Assert
        mockMvc.perform(post("/api/users/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(expectedId.toString())) // jsonPath verifica os campos do JSON de retorno
                .andExpect(jsonPath("$.email").value(createDTO.email()))
                .andExpect(jsonPath("$.mfaEnable").value(false));

        verify(userService).createUser(any(UserCreateDTO.class));
    }

    // --- TESTES PARA GET /listar ---

    @Test
    @DisplayName("Deve retornar 200 OK e uma lista de usuários")
    void getAllUsers_Success() throws Exception {
        // Arrange
        UserResponseDTO user1 = new UserResponseDTO(UUID.randomUUID(), "user1@teste.com", false, LocalDateTime.now());
        UserResponseDTO user2 = new UserResponseDTO(UUID.randomUUID(), "user2@teste.com", true, LocalDateTime.now());

        when(userService.getAllUsers()).thenReturn(List.of(user1, user2));

        // Act & Assert
        mockMvc.perform(get("/api/users/listar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(2)) // Verifica se o array JSON retornou com tamanho 2
                .andExpect(jsonPath("$[0].email").value(user1.email())) // Acessa o primeiro item do array JSON
                .andExpect(jsonPath("$[1].email").value(user2.email()));

        verify(userService).getAllUsers();
    }

    // --- TESTES PARA GET /{id} ---

    @Test
    @DisplayName("Deve retornar 200 OK e o usuário correspondente ao ID")
    void getUserById_Success() throws Exception {
        // Arrange
        UUID userId = UUID.randomUUID();
        UserResponseDTO responseDTO = new UserResponseDTO(userId, "busca@teste.com", false, LocalDateTime.now());

        when(userService.getUserById(userId)).thenReturn(responseDTO);

        // Act & Assert
        mockMvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.email").value(responseDTO.email()));

        verify(userService).getUserById(userId);
    }

    // --- TESTES PARA DELETE /delete/{id} ---

    @Test
    @DisplayName("Deve retornar 200 OK ao deletar o usuário com sucesso")
    void deleteUser_Success() throws Exception {
        // Arrange
        UUID userId = UUID.randomUUID();

        // Act & Assert
        mockMvc.perform(delete("/api/users/delete/{id}", userId))
                .andExpect(status().isOk());

        verify(userService).deleteUser(userId);
    }
}