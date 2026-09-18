package com.pulsewatch.backend.auth.controller;

import com.pulsewatch.backend.auth.entity.User;
import com.pulsewatch.backend.auth.entity.Workspace;
import com.pulsewatch.backend.auth.entity.WorkspaceMember;
import com.pulsewatch.backend.auth.repository.UserRepository;
import com.pulsewatch.backend.auth.repository.WorkspaceMemberRepository;
import com.pulsewatch.backend.auth.repository.WorkspaceRepository;
import com.pulsewatch.backend.auth.security.jwt.JwtUtils;
import com.pulsewatch.backend.auth.service.RefreshTokenService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * GitHub OAuth2 login controller.
 *
 * Flow:
 *   1. Frontend calls GET /api/auth/github/url  → gets the GitHub authorization URL
 *   2. User is redirected to GitHub, authorizes the app
 *   3. GitHub redirects to GET /oauth2/callback/github?code=... (frontend page)
 *   4. Frontend calls POST /api/auth/github/callback with the code
 *   5. Backend exchanges code → access_token → user info → returns JWT
 */
@RestController
@RequestMapping("/api/auth/github")
@CrossOrigin(origins = "*", maxAge = 3600)
public class GitHubOAuthController {

    private static final Logger logger = LoggerFactory.getLogger(GitHubOAuthController.class);

    @Value("${github.client-id:}") private String clientId;
    @Value("${github.client-secret:}") private String clientSecret;
    @Value("${github.redirect-uri:http://localhost/oauth2/callback/github}") private String redirectUri;

    @Autowired private UserRepository userRepository;
    @Autowired private WorkspaceRepository workspaceRepository;
    @Autowired private WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired private JwtUtils jwtUtils;
    @Autowired private RefreshTokenService refreshTokenService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private WebClient.Builder webClientBuilder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Step 1: Return the GitHub authorization URL for the frontend to redirect to. */
    @GetMapping("/url")
    public ResponseEntity<?> getAuthorizationUrl() {
        if (clientId == null || clientId.isBlank()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "GitHub OAuth is not configured"));
        }
        String url = "https://github.com/login/oauth/authorize"
                + "?client_id=" + clientId
                + "&redirect_uri=" + redirectUri
                + "&scope=user:email"
                + "&state=" + UUID.randomUUID();
        return ResponseEntity.ok(Map.of("url", url));
    }

    /** Step 2: Exchange the GitHub code for a JWT. Called by the frontend after redirect. */
    @PostMapping("/callback")
    @Transactional
    public ResponseEntity<?> handleCallback(@RequestBody Map<String, String> body) {
        String code = body.get("code");
        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing code parameter"));
        }

        try {
            // 1. Exchange code for GitHub access token
            String tokenResponse = webClientBuilder.build()
                    .post()
                    .uri("https://github.com/login/oauth/access_token")
                    .header("Accept", "application/json")
                    .body(BodyInserters.fromFormData("client_id", clientId)
                            .with("client_secret", clientSecret)
                            .with("code", code)
                            .with("redirect_uri", redirectUri))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode tokenJson = objectMapper.readTree(tokenResponse);
            String accessToken = tokenJson.path("access_token").asText();

            if (accessToken == null || accessToken.isBlank()) {
                String ghError = tokenJson.path("error_description").asText("Unknown GitHub OAuth error");
                logger.error("GitHub token exchange failed: {}", ghError);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "GitHub OAuth failed: " + ghError));
            }

            // 2. Fetch GitHub user info
            String userResponse = webClientBuilder.build()
                    .get()
                    .uri("https://api.github.com/user")
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Accept", "application/vnd.github+json")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode userJson = objectMapper.readTree(userResponse);

            // 3. Get primary email if not public on profile
            String email = userJson.path("email").asText(null);
            if (email == null || email.isBlank()) {
                email = fetchPrimaryEmail(accessToken);
            }
            if (email == null || email.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "No email found on GitHub account. Please make your email public or add a primary email."));
            }

            String githubLogin = userJson.path("login").asText("github_user");
            String displayName  = userJson.path("name").asText(githubLogin);

            // 4. Find or create user
            final String finalEmail = email.toLowerCase().trim();
            Optional<User> existing = userRepository.findByEmail(finalEmail);
            User user;
            UUID workspaceId;
            String workspaceRole;

            if (existing.isPresent()) {
                user = existing.get();
                List<WorkspaceMember> memberships = workspaceMemberRepository.findByUserId(user.getId());
                workspaceId   = memberships.isEmpty() ? null : memberships.get(0).getWorkspaceId();
                workspaceRole = memberships.isEmpty() ? "VIEWER" : memberships.get(0).getRole();
            } else {
                // Create new user — GitHub OAuth users get a random unguessable password
                user = new User(finalEmail, passwordEncoder.encode(UUID.randomUUID().toString()), "USER");
                userRepository.save(user);

                String slug = finalEmail.replace("@", "-").replace(".", "-");
                Workspace workspace = new Workspace(displayName + "'s Workspace", slug);
                workspaceRepository.save(workspace);

                WorkspaceMember member = new WorkspaceMember(workspace.getId(), user.getId(), "OWNER");
                workspaceMemberRepository.save(member);

                workspaceId   = workspace.getId();
                workspaceRole = "OWNER";
                logger.info("Created new user via GitHub OAuth: {}", finalEmail);
            }

            // 5. Issue JWT
            String jwt = jwtUtils.generateTokenFromUsername(user.getEmail(), workspaceId, workspaceRole);
            com.pulsewatch.backend.auth.entity.RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

            return ResponseEntity.ok(Map.of(
                    "token", jwt,
                    "refreshToken", refreshToken.getToken(),
                    "email", user.getEmail(),
                    "role", user.getRole(),
                    "workspaceId", workspaceId != null ? workspaceId.toString() : "",
                    "workspaceRole", workspaceRole,
                    "provider", "github"
            ));

        } catch (Exception e) {
            logger.error("GitHub OAuth callback error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "OAuth processing failed: " + e.getMessage()));
        }
    }

    /** Fetch the user's primary verified email via the GitHub emails API. */
    private String fetchPrimaryEmail(String accessToken) {
        try {
            String emailsResponse = webClientBuilder.build()
                    .get()
                    .uri("https://api.github.com/user/emails")
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Accept", "application/vnd.github+json")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode emails = objectMapper.readTree(emailsResponse);
            // Prefer primary + verified email
            for (JsonNode e : emails) {
                if (e.path("primary").asBoolean() && e.path("verified").asBoolean()) {
                    return e.path("email").asText(null);
                }
            }
            // Fallback: first verified
            for (JsonNode e : emails) {
                if (e.path("verified").asBoolean()) {
                    return e.path("email").asText(null);
                }
            }
        } catch (Exception ex) {
            logger.warn("Could not fetch GitHub emails: {}", ex.getMessage());
        }
        return null;
    }
}
