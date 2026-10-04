package com.example.taskmanager.repository;

import com.example.taskmanager.model.Role;
import com.example.taskmanager.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepositoryTest {

    @Autowired private UserRepository userRepository;

    private User build(String email) {
        User u = new User();
        u.setEmail(email);
        u.setPassword("x");
        u.setRole(Role.ROLE_USER);
        return u;
    }

    @Test
    void save_shouldPersist() {
        User saved = userRepository.save(build("a@b.com"));
        assertThat(saved.getId()).isNotNull();
    }

    @Test
    void findByEmail_whenExists_shouldReturn() {
        userRepository.save(build("find@test.com"));
        Optional<User> found = userRepository.findByEmail("find@test.com");
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("find@test.com");
    }

    @Test
    void findByEmail_whenNotExists_shouldReturnEmpty() {
        assertThat(userRepository.findByEmail("nope@test.com")).isEmpty();
    }
}