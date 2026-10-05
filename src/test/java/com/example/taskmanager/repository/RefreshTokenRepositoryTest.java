package com.example.taskmanager.repository;

import com.example.taskmanager.model.RefreshToken;
import com.example.taskmanager.model.Role;
import com.example.taskmanager.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class RefreshTokenRepositoryTest {

    @Autowired private RefreshTokenRepository repo;
    @Autowired private UserRepository userRepository;

    private User saveUser(String email) {
        User u = new User();
        u.setEmail(email);
        u.setPassword("x");
        u.setRole(Role.ROLE_USER);
        return userRepository.save(u);
    }

    private RefreshToken build(User user, String token) {
        RefreshToken rt = new RefreshToken();
        rt.setToken(token);
        rt.setUser(user);
        rt.setExpiresAt(LocalDateTime.now().plusDays(7));
        return rt;
    }

    @Test
    void save_shouldPersist() {
        User user = saveUser("rt@test.com");
        RefreshToken saved = repo.save(build(user, "token-abc"));

        assertThat(saved.getId()).isNotNull();
    }

    @Test
    void findByToken_whenExists_shouldReturn() {
        User user = saveUser("rt@test.com");
        repo.save(build(user, "find-me"));

        Optional<RefreshToken> found = repo.findByToken("find-me");

        assertThat(found).isPresent();
        assertThat(found.get().getToken()).isEqualTo("find-me");
        assertThat(found.get().getUser().getEmail()).isEqualTo("rt@test.com");
    }

    @Test
    void findByToken_whenNotExists_shouldReturnEmpty() {
        assertThat(repo.findByToken("nonexistent")).isEmpty();
    }

    @Test
    void deleteByUserId_shouldRemoveAllUserTokens() {
        User user = saveUser("rt@test.com");
        repo.save(build(user, "token-1"));
        repo.save(build(user, "token-2"));

        repo.deleteByUserId(user.getId());

        assertThat(repo.findByToken("token-1")).isEmpty();
        assertThat(repo.findByToken("token-2")).isEmpty();
    }
}