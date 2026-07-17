package com.pulsewatch.backend.auth.service;

import com.pulsewatch.backend.audit.service.AuditLogService;
import com.pulsewatch.backend.auth.dto.LoginRequest;
import com.pulsewatch.backend.auth.dto.RegisterRequest;
import com.pulsewatch.backend.auth.entity.RefreshToken;
import com.pulsewatch.backend.auth.entity.User;
import com.pulsewatch.backend.auth.entity.Workspace;
import com.pulsewatch.backend.auth.entity.WorkspaceMember;
import com.pulsewatch.backend.auth.repository.UserRepository;
import com.pulsewatch.backend.auth.repository.WorkspaceRepository;
import com.pulsewatch.backend.auth.repository.WorkspaceMemberRepository;
import com.pulsewatch.backend.auth.security.jwt.JwtUtils;
import com.pulsewatch.backend.auth.security.services.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuthService {

    @Autowired private AuthenticationManager authenticationManager;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder encoder;
    @Autowired private JwtUtils jwtUtils;
    @Autowired private RefreshTokenService refreshTokenService;
    @Autowired private AuditLogService auditLogService;
    @Autowired private WorkspaceRepository workspaceRepository;
    @Autowired private WorkspaceMemberRepository workspaceMemberRepository;

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Transactional
    public void registerUser(RegisterRequest registerRequest) {
        User user = new User(
                registerRequest.getEmail(),
                encoder.encode(registerRequest.getPassword()),
                "USER"
        );
        userRepository.save(user);
        auditLogService.log(user.getId(), "REGISTER", "USER", user.getId());

        // Create default personal workspace
        String slug = user.getEmail().replace("@", "-").replace(".", "-");
        Workspace workspace = new Workspace(user.getEmail() + "'s Personal Workspace", slug);
        workspaceRepository.save(workspace);

        // Associate user as OWNER of the workspace
        WorkspaceMember member = new WorkspaceMember(workspace.getId(), user.getId(), "OWNER");
        workspaceMemberRepository.save(member);
    }

    @Transactional
    public AuthenticationResult authenticateUser(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        String jwt = jwtUtils.generateJwtToken(authentication);

        String role = userDetails.getAuthorities().stream()
                .findFirst().map(a -> a.getAuthority().replace("ROLE_", "")).orElse("USER");

        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        auditLogService.log(user.getId(), "LOGIN", "USER", user.getId());

        return new AuthenticationResult(jwt, refreshToken.getToken(), userDetails.getUsername(), role, userDetails.getWorkspaceId(), userDetails.getWorkspaceRole());
    }

    @Transactional
    public void logoutUser(UserDetailsImpl userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        refreshTokenService.deleteByUser(user);
        auditLogService.log(user.getId(), "LOGOUT", "USER", user.getId());
    }

    public static class AuthenticationResult {
        public final String jwt;
        public final String refreshToken;
        public final String email;
        public final String role;
        public final UUID workspaceId;
        public final String workspaceRole;

        public AuthenticationResult(String jwt, String refreshToken, String email, String role, UUID workspaceId, String workspaceRole) {
            this.jwt = jwt;
            this.refreshToken = refreshToken;
            this.email = email;
            this.role = role;
            this.workspaceId = workspaceId;
            this.workspaceRole = workspaceRole;
        }
    }
}
