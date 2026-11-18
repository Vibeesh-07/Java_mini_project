package com.refind.controller;

import com.refind.dto.auth.AuthResponse;
import com.refind.dto.auth.LoginRequest;
import com.refind.dto.auth.RegisterRequest;
import com.refind.dto.auth.UserResponse;
import com.refind.security.CustomUserDetails;
import com.refind.security.SecurityUtils;
import com.refind.service.UserService;
import com.refind.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;

    public AuthController(UserService userService, AuthenticationManager authenticationManager) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/api/auth/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = userService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping({"/api/auth/login", "/login"})
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        String email = request.getEmail().trim().toLowerCase();
        if (!userService.accountExists(email)) {
            throw new UnauthorizedException("No account exists for this email address. Please create an account first.");
        }
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.getPassword())
        );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        HttpSession session = httpRequest.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        UserResponse userResponse = UserResponse.fromEntity(userDetails.getUser());

        return ResponseEntity.ok(new AuthResponse("Login successful", true, userResponse));
    }

    @PostMapping({"/api/auth/logout", "/logout"})
    public ResponseEntity<Map<String, Object>> logout(HttpServletRequest request, HttpServletResponse response) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("message", "Logged out successfully");
        resp.put("authenticated", false);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/api/auth/csrf")
    public ResponseEntity<Map<String, String>> getCsrfToken(CsrfToken csrfToken) {
        Map<String, String> response = new HashMap<>();
        if (csrfToken != null) {
            response.put("token", csrfToken.getToken());
            response.put("headerName", csrfToken.getHeaderName());
            response.put("parameterName", csrfToken.getParameterName());
        } else {
            response.put("token", "");
            response.put("headerName", "X-XSRF-TOKEN");
            response.put("parameterName", "_csrf");
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/auth/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser() {
        Map<String, Object> response = new HashMap<>();
        return SecurityUtils.getCurrentUserDetails()
                .map(details -> {
                    response.put("authenticated", true);
                    response.put("user", UserResponse.fromEntity(details.getUser()));
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {
                    response.put("authenticated", false);
                    response.put("user", null);
                    return ResponseEntity.ok(response);
                });
    }
}
