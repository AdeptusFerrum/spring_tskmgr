package com.example.taskmanager.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    void getUsername_shouldReturnEmail() {
        User u = new User();
        u.setEmail("a@b.com");
        assertThat(u.getUsername()).isEqualTo("a@b.com");
    }

    @Test
    void getAuthorities_shouldReturnRole() {
        User u = new User();
        u.setRole(Role.ROLE_ADMIN);
        assertThat(u.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void allBooleanFlags_shouldBeTrue() {
        User u = new User();
        assertThat(u.isAccountNonExpired()).isTrue();
        assertThat(u.isAccountNonLocked()).isTrue();
        assertThat(u.isCredentialsNonExpired()).isTrue();
        assertThat(u.isEnabled()).isTrue();
    }
}