package com.pulsewatch.backend.auth.security.services;

import com.pulsewatch.backend.auth.entity.WorkspaceMember;
import com.pulsewatch.backend.auth.entity.User;
import com.pulsewatch.backend.auth.repository.UserRepository;
import com.pulsewatch.backend.auth.repository.WorkspaceMemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    
    @Autowired
    UserRepository userRepository;

    @Autowired
    WorkspaceMemberRepository workspaceMemberRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with email: " + email));

        List<WorkspaceMember> memberships = workspaceMemberRepository.findByUserId(user.getId());
        UUID activeWorkspaceId = null;
        String role = "OWNER";
        if (!memberships.isEmpty()) {
            WorkspaceMember primary = memberships.get(0);
            activeWorkspaceId = primary.getWorkspaceId();
            role = primary.getRole();
        }

        return UserDetailsImpl.build(user, activeWorkspaceId, role);
    }
}
