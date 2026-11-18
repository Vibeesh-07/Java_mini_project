package com.refind.service;

import com.refind.dto.auth.RegisterRequest;
import com.refind.dto.auth.UserResponse;
import com.refind.entity.Role;
import com.refind.entity.User;
import com.refind.exception.DuplicateResourceException;
import com.refind.exception.ResourceNotFoundException;
import com.refind.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String collegeEmailDomain;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       @Value("${app.college.email-domain:psgitech.ac.in}") String collegeEmailDomain) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.collegeEmailDomain = collegeEmailDomain.trim().toLowerCase();
    }

    /** Kept for focused unit tests that instantiate the service without Spring configuration. */
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this(userRepository, passwordEncoder, "campus.edu");
    }

    @Transactional
    public UserResponse registerUser(RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (!normalizedEmail.endsWith("@" + collegeEmailDomain)) {
            throw new com.refind.exception.BadRequestException("Please register with your " + collegeEmailDomain + " college email address");
        }
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateResourceException("An account is already registered with email: " + normalizedEmail);
        }

        User user = new User();
        user.setName(request.getName().trim());
        user.setEmail(normalizedEmail);
        user.setContactPhone(normalizeOptional(request.getContactPhone()));
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.ROLE_USER);

        User saved = userRepository.save(user);
        return UserResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public User getUserEntityById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        return UserResponse.fromEntity(getUserEntityById(id));
    }

    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        return userRepository.findByEmail(email.trim().toLowerCase())
                .map(UserResponse::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    @Transactional(readOnly = true)
    public boolean accountExists(String email) {
        return email != null && userRepository.existsByEmail(email.trim().toLowerCase());
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
