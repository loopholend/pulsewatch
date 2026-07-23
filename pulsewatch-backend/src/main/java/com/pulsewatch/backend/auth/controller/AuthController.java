package com.pulsewatch.backend.auth.controller;

import com.pulsewatch.backend.audit.service.AuditLogService;
import com.pulsewatch.backend.auth.dto.*;
import com.pulsewatch.backend.auth.entity.RefreshToken;
import com.pulsewatch.backend.auth.entity.User;
import com.pulsewatch.backend.auth.repository.UserRepository;
import com.pulsewatch.backend.auth.security.jwt.JwtUtils;
import com.pulsewatch.backend.auth.security.services.UserDetailsImpl;
import com.pulsewatch.backend.auth.service.RefreshTokenService;
import com.pulsewatch.backend.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Authentication management APIs")
public class AuthController {

    @Autowired private AuthService authService;
    @Autowired private RefreshTokenService refreshTokenService;
    @Autowired private JwtUtils jwtUtils;
    @Autowired private com.pulsewatch.backend.auth.repository.WorkspaceMemberRepository workspaceMemberRepository;

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        if (authService.existsByEmail(registerRequest.getEmail())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email is already in use"));
        }

        authService.registerUser(registerRequest);
        return ResponseEntity.ok(Map.of("message", "User registered successfully"));
    }

    @PostMapping("/login")
    @Operation(summary = "Login and get JWT tokens")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        AuthService.AuthenticationResult result = authService.authenticateUser(loginRequest);
        return ResponseEntity.ok(new JwtResponse(result.jwt, result.refreshToken, result.email, result.role, result.workspaceId, result.workspaceRole));
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Refresh the access token")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return refreshTokenService.findByToken(request.getRefreshToken())
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    java.util.List<com.pulsewatch.backend.auth.entity.WorkspaceMember> memberships = workspaceMemberRepository.findByUserId(user.getId());
                    java.util.UUID activeWorkspaceId = null;
                    // Use the user's actual role in their workspace — never hardcode OWNER
                    String role = "VIEWER";
                    if (!memberships.isEmpty()) {
                        com.pulsewatch.backend.auth.entity.WorkspaceMember primary = memberships.get(0);
                        activeWorkspaceId = primary.getWorkspaceId();
                        role = primary.getRole();
                    }
                    String token = jwtUtils.generateTokenFromUsername(user.getEmail(), activeWorkspaceId, role);
                    return ResponseEntity.ok(Map.of("accessToken", token, "tokenType", "Bearer"));
                })
                .orElseThrow(() -> new RuntimeException("Refresh token not found"));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout the current user")
    public ResponseEntity<?> logoutUser() {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        authService.logoutUser(userDetails);
        return ResponseEntity.ok(Map.of("message", "Logout successful"));
    }
}
