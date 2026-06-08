package com.adhamamr.passwordy.security;

import com.adhamamr.passwordy.model.User;
import com.adhamamr.passwordy.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Bridges Spring Security's {@link UserDetailsService} contract to {@link com.adhamamr.passwordy.repository.UserRepository}.
 * Used by the authentication manager to load a user by username during token validation.
 * No roles or authorities are assigned — all users have the same level of access.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getMasterPasswordHash(),
                List.of()
        );
    }
}