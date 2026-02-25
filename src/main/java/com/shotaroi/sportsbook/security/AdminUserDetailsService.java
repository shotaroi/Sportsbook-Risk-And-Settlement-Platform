package com.shotaroi.sportsbook.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Admin user for Basic Auth. Password in config: use {noop}plain for dev, or bcrypt hash for prod.
 */
@Service
public class AdminUserDetailsService implements UserDetailsService {

    private final String adminUsername;
    private final String adminPassword;

    public AdminUserDetailsService(
            @Value("${admin.username}") String adminUsername,
            @Value("${admin.password}") String adminPassword
    ) {
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (!adminUsername.equals(username)) {
            throw new UsernameNotFoundException("Admin not found: " + username);
        }
        return User.builder()
                .username(adminUsername)
                .password(adminPassword)  // Use {noop}admin-secret for dev, or bcrypt hash for prod
                .roles("ADMIN")
                .build();
    }
}
