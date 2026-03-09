package com.accenture.security;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class TokenAuthenticationService {

    private static final Map<String, UserDetails> TOKENS = Map.of(
            "f4b8c9c7c3e14f0e8a3d9c71d2e7c6c5",
            new User(
                    "user",
                    "",
                    List.of(new SimpleGrantedAuthority("ROLE_USER"))
            ),
            "a8c1e0c7-1e9f-4d9b-b4a1-7c2e91c3a0e2",
            new User(
                    "admin",
                    "",
                    List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
            )
    );

    public UserDetails findByToken(String token) {
        return TOKENS.get(token);
    }
}