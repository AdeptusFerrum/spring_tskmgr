package com.example.taskmanager.service;

import com.example.taskmanager.dto.AuthResponse;
import com.example.taskmanager.dto.LoginRequest;
import com.example.taskmanager.dto.RegisterRequest;
import com.example.taskmanager.model.RefreshToken;
import com.example.taskmanager.model.Role;
import com.example.taskmanager.model.User;
import com.example.taskmanager.repository.UserRepository;
import com.example.taskmanager.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private RefreshTokenService refreshTokenService;

    @InjectMocks private AuthService authService;

    // ========== REGISTER ==========

    @Test
    void register_whenEmailNew_shouldSaveUserAndReturnTokens() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("new@test.com");
        req.setPassword("pass123");

        when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("pass123")).thenReturn("hashed");
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");

        RefreshToken rt = new RefreshToken();
        rt.setToken("refresh-uuid");
        when(refreshTokenService.create(any(User.class))).thenReturn(rt);

        AuthResponse response = authService.register(req);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-uuid");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_shouldSetRoleUser() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("new@test.com");
        req.setPassword("pass123");

        when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt");

        RefreshToken rt = new RefreshToken();
        rt.setToken("r");
        when(refreshTokenService.create(any(User.class))).thenReturn(rt);

        authService.register(req);

        verify(userRepository).save(argThat(user ->
                user.getRole() == Role.ROLE_USER
                        && user.getEmail().equals("new@test.com")
                        && user.getPassword().equals("hashed")));
    }

    @Test
    void register_whenEmailExists_shouldThrow() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("dup@test.com");
        req.setPassword("pass");

        User existing = new User();
        existing.setEmail("dup@test.com");
        when(userRepository.findByEmail("dup@test.com")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already registered");
    }

    // ========== LOGIN ==========

    @Test
    void login_withValidCredentials_shouldReturnTokens() {
        LoginRequest req = new LoginRequest();
        req.setEmail("user@test.com");
        req.setPassword("pass");

        User user = new User();
        user.setEmail("user@test.com");
        user.setRole(Role.ROLE_USER);
        user.setPassword("hashed");

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("access-token");

        RefreshToken rt = new RefreshToken();
        rt.setToken("refresh");
        when(refreshTokenService.create(user)).thenReturn(rt);

        AuthResponse response = authService.login(req);

        assertThat(response.getToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh");
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void login_whenBadCredentials_shouldThrow() {
        LoginRequest req = new LoginRequest();
        req.setEmail("user@test.com");
        req.setPassword("wrong");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_whenUserNotFound_shouldThrow() {
        LoginRequest req = new LoginRequest();
        req.setEmail("missing@test.com");
        req.setPassword("pass");

        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");
    }

    // ========== REFRESH ==========

    @Test
    void refresh_withValidToken_shouldReturnNewAccessToken() {
        User user = new User();
        user.setEmail("user@test.com");

        RefreshToken rt = new RefreshToken();
        rt.setToken("refresh-uuid");
        rt.setUser(user);

        when(refreshTokenService.verify("refresh-uuid")).thenReturn(rt);
        when(jwtService.generateToken(user)).thenReturn("new-access");

        AuthResponse response = authService.refresh("refresh-uuid");

        assertThat(response.getToken()).isEqualTo("new-access");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-uuid");
    }

    // ========== LOGOUT ==========

    @Test
    void logout_shouldCallDeleteByUserId() {
        authService.logout(42L);
        verify(refreshTokenService).deleteByUserId(42L);
    }
}