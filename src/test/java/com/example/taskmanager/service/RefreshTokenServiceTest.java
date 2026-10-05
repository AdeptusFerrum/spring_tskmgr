package com.example.taskmanager.service;

import com.example.taskmanager.model.RefreshToken;
import com.example.taskmanager.model.User;
import com.example.taskmanager.repository.RefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock private RefreshTokenRepository repo;

    @InjectMocks private RefreshTokenService service;

    private User buildUser() {
        User u = new User();
        u.setId(1L);
        u.setEmail("user@test.com");
        return u;
    }

    @Test
    void create_shouldGenerateUniqueTokenAndSetExpiryInFuture() {
        User user = buildUser();
        when(repo.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        RefreshToken result = service.create(user);

        assertThat(result.getToken()).isNotBlank();
        assertThat(result.getUser()).isEqualTo(user);
        assertThat(result.getExpiresAt()).isAfter(LocalDateTime.now().plusDays(6));
        assertThat(result.getExpiresAt()).isBefore(LocalDateTime.now().plusDays(8));
        verify(repo).save(any(RefreshToken.class));
    }

    @Test
    void create_shouldGenerateDifferentTokensForDifferentCalls() {
        User user = buildUser();
        when(repo.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        RefreshToken t1 = service.create(user);
        RefreshToken t2 = service.create(user);

        assertThat(t1.getToken()).isNotEqualTo(t2.getToken());
    }

    @Test
    void verify_whenTokenNotFound_shouldThrow() {
        when(repo.findByToken("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verify("nonexistent"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void verify_whenTokenExpired_shouldDeleteAndThrow() {
        RefreshToken expired = new RefreshToken();
        expired.setToken("old");
        expired.setExpiresAt(LocalDateTime.now().minusDays(1));

        when(repo.findByToken("old")).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.verify("old"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expired");

        verify(repo).delete(expired);
    }

    @Test
    void verify_whenTokenValid_shouldReturnIt() {
        RefreshToken valid = new RefreshToken();
        valid.setToken("valid");
        valid.setExpiresAt(LocalDateTime.now().plusDays(5));

        when(repo.findByToken("valid")).thenReturn(Optional.of(valid));

        RefreshToken result = service.verify("valid");

        assertThat(result).isEqualTo(valid);
        verify(repo, never()).delete(any());
    }

    @Test
    void deleteByUserId_shouldCallRepository() {
        service.deleteByUserId(1L);
        verify(repo).deleteByUserId(1L);
    }
}