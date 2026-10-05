package com.example.taskmanager.config;

import com.example.taskmanager.model.Role;
import com.example.taskmanager.model.User;
import com.example.taskmanager.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DataInitializerTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @Test
    void run_whenBothMissing_shouldCreateAdminAndUser() {
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("hashed");

        DataInitializer init = new DataInitializer(userRepository, passwordEncoder);
        init.run();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(2)).save(captor.capture());

        List<User> saved = captor.getAllValues();
        assertThat(saved).extracting(User::getEmail)
                .containsExactlyInAnyOrder("admin@test.com", "user@test.com");
        assertThat(saved).extracting(User::getRole)
                .containsExactlyInAnyOrder(Role.ROLE_ADMIN, Role.ROLE_USER);
    }

    @Test
    void run_whenAdminExists_shouldCreateOnlyUser() {
        User existing = new User();
        existing.setEmail("admin@test.com");
        existing.setRole(Role.ROLE_ADMIN);
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(existing));
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("hashed");

        DataInitializer init = new DataInitializer(userRepository, passwordEncoder);
        init.run();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(captor.capture());

        assertThat(captor.getValue().getEmail()).isEqualTo("user@test.com");
        assertThat(captor.getValue().getRole()).isEqualTo(Role.ROLE_USER);
    }

    @Test
    void run_whenBothExist_shouldNotCreateAnything() {
        User admin = new User(); admin.setEmail("admin@test.com");
        User user = new User(); user.setEmail("user@test.com");
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));

        DataInitializer init = new DataInitializer(userRepository, passwordEncoder);
        init.run();

        verify(userRepository, never()).save(any());
    }
}