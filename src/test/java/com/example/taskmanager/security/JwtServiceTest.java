package com.example.taskmanager.security;

import com.example.taskmanager.model.Role;
import com.example.taskmanager.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setup() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret",
                "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(jwtService, "expiration", 86400000L);

        user = new User();
        user.setId(1L);
        user.setEmail("test@test.com");
        user.setPassword("x");
        user.setRole(Role.ROLE_USER);
    }

    @Test
    void generateToken_shouldContainUsername() {
        String token = jwtService.generateToken(user);
        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUsername(token)).isEqualTo("test@test.com");
    }

    @Test
    void isTokenValid_withValidToken_shouldReturnTrue() {
        String token = jwtService.generateToken(user);
        assertThat(jwtService.isTokenValid(token, user)).isTrue();
    }

    @Test
    void isTokenValid_withDifferentUser_shouldReturnFalse() {
        String token = jwtService.generateToken(user);

        User other = new User();
        other.setEmail("other@test.com");
        other.setPassword("x");
        other.setRole(Role.ROLE_USER);

        assertThat(jwtService.isTokenValid(token, other)).isFalse();
    }
}