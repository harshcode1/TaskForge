package com.projectmgmttool.backend.service;

import com.projectmgmttool.backend.dto.AuthRequest;
import com.projectmgmttool.backend.dto.AuthResponse;
import com.projectmgmttool.backend.dto.RegisterRequest;
import com.projectmgmttool.backend.entity.User;
import com.projectmgmttool.backend.entity.enums.Role;
import com.projectmgmttool.backend.exception.CustomApiException;
import com.projectmgmttool.backend.repository.UserRepository;
import com.projectmgmttool.backend.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AuthenticationManager authenticationManager;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setRole(Role.MEMBER);
    }

    @Test
    void register_newUser_returnsAuthResponse() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Test User");
        request.setEmail("test@example.com");
        request.setPassword("Password123!");
        request.setRole(Role.MEMBER);

        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtUtil.generateToken(any(User.class))).thenReturn("test-token");

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("test-token", response.getToken());
        assertEquals("test@example.com", response.getEmail());
        assertEquals(Role.MEMBER, response.getRole());
    }

    @Test
    void register_duplicateEmail_throwsException() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setName("Test");
        request.setPassword("Password123!");

        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        CustomApiException ex = assertThrows(CustomApiException.class, () -> authService.register(request));
        assertEquals(409, ex.getErrorCode());
    }

    @Test
    void login_validCredentials_returnsAuthResponse() {
        AuthRequest request = new AuthRequest();
        request.setEmail("test@example.com");
        request.setPassword("Password123!");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(jwtUtil.generateToken(testUser)).thenReturn("test-token");

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("test-token", response.getToken());
        assertEquals("test@example.com", response.getEmail());
    }
}
