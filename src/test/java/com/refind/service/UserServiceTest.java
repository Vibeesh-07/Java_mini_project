package com.refind.service;

import com.refind.dto.auth.RegisterRequest;
import com.refind.dto.auth.UserResponse;
import com.refind.entity.Role;
import com.refind.entity.User;
import com.refind.exception.DuplicateResourceException;
import com.refind.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder);
    }

    @Test
    @DisplayName("Register user hashes password with BCrypt and creates ROLE_USER")
    void testRegisterUserSuccess() {
        RegisterRequest req = new RegisterRequest("Test Student", "student@campus.edu", "PlainSecret123");

        when(userRepository.existsByEmail("student@campus.edu")).thenReturn(false);
        when(passwordEncoder.encode("PlainSecret123")).thenReturn("$2a$12$hashedPasswordExample");

        User savedUser = new User("Test Student", "student@campus.edu", "$2a$12$hashedPasswordExample", Role.ROLE_USER);
        savedUser.setId(42L);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserResponse response = userService.registerUser(req);

        assertNotNull(response);
        assertEquals(42L, response.getId());
        assertEquals("Test Student", response.getName());
        assertEquals("student@campus.edu", response.getEmail());
        assertEquals(Role.ROLE_USER, response.getRole());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User captured = captor.getValue();
        assertEquals("$2a$12$hashedPasswordExample", captured.getPasswordHash(), "Plain password must not be stored");
    }

    @Test
    @DisplayName("Register user throws DuplicateResourceException on duplicate email")
    void testRegisterUserDuplicateEmailThrowsException() {
        RegisterRequest req = new RegisterRequest("Duplicate User", "existing@campus.edu", "Secret123");
        when(userRepository.existsByEmail("existing@campus.edu")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> userService.registerUser(req));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Lookup user by email returns sanitized UserResponse")
    void testGetUserByEmail() {
        User user = new User("Jane Doe", "jane@campus.edu", "hash123", Role.ROLE_USER);
        user.setId(5L);
        when(userRepository.findByEmail("jane@campus.edu")).thenReturn(Optional.of(user));

        UserResponse res = userService.getUserByEmail("jane@campus.edu");
        assertNotNull(res);
        assertEquals(5L, res.getId());
        assertEquals("Jane Doe", res.getName());
    }
}
