package com.pulsewatch.backend.auth.security.jwt;

import com.pulsewatch.backend.auth.entity.ApiKey;
import com.pulsewatch.backend.auth.entity.User;
import com.pulsewatch.backend.auth.repository.UserRepository;
import com.pulsewatch.backend.auth.security.services.UserDetailsImpl;
import com.pulsewatch.backend.auth.service.ApiKeyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.pulsewatch.backend.auth.entity.WorkspaceMember;
import com.pulsewatch.backend.auth.repository.WorkspaceMemberRepository;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    @Autowired
    private ApiKeyService apiKeyService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkspaceMemberRepository workspaceMemberRepository;

    private static final Logger logger = LoggerFactory.getLogger(ApiKeyAuthFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String apiKey = parseApiKey(request);
            if (apiKey != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                Optional<ApiKey> validatedKey = apiKeyService.validateKey(apiKey);
                if (validatedKey.isPresent()) {
                    ApiKey key = validatedKey.get();
                    UUID workspaceId = key.getWorkspaceId();
                    UUID creatorUserId = key.getCreatedBy();

                    if (creatorUserId == null) {
                        // Legacy key with no createdBy: refuse rather than silently grant OWNER privileges
                        logger.warn("API key {} has no createdBy - authentication refused to prevent privilege escalation", key.getId());
                        filterChain.doFilter(request, response);
                        return;
                    }

                    Optional<User> userOpt = userRepository.findById(creatorUserId);
                    if (userOpt.isPresent()) {
                        // Fetch the creator's CURRENT role in this workspace from DB (live, not stored in key)
                        Optional<WorkspaceMember> memberOpt =
                                workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, creatorUserId);
                        String role = memberOpt.map(WorkspaceMember::getRole).orElse("VIEWER");

                        UserDetails userDetails = UserDetailsImpl.build(userOpt.get(), workspaceId, role);
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Cannot set user API key authentication: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String parseApiKey(HttpServletRequest request) {
        String key = request.getHeader("X-API-Key");
        if (StringUtils.hasText(key)) {
            return key.trim();
        }
        return null;
    }
}
