package com.example.taskmanager.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenTest {

    @Test
    void settersAndGetters_shouldWork() {
        RefreshToken rt = new RefreshToken();
        User user = new User();
        user.setEmail("u@test.com");
        LocalDateTime expires = LocalDateTime.now().plusDays(7);

        rt.setId(1L);
        rt.setToken("abc-123");
        rt.setUser(user);
        rt.setExpiresAt(expires);

        assertThat(rt.getId()).isEqualTo(1L);
        assertThat(rt.getToken()).isEqualTo("abc-123");
        assertThat(rt.getUser()).isEqualTo(user);
        assertThat(rt.getExpiresAt()).isEqualTo(expires);
    }
}