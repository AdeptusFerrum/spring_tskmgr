package com.example.taskmanager.service;

import com.example.taskmanager.dto.AuthResponse;
import com.example.taskmanager.dto.LoginRequest;
import com.example.taskmanager.dto.RegisterRequest;
import com.example.taskmanager.model.RefreshToken;
import com.example.taskmanager.model.Role;
import com.example.taskmanager.model.User;
import com.example.taskmanager.repository.UserRepository;
import com.example.taskmanager.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager,
                       RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already registered");
        }
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.ROLE_USER);
        userRepository.save(user);

        String access = jwtService.generateToken(user);
        RefreshToken rt = refreshTokenService.create(user);
        return new AuthResponse(access, rt.getToken());
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String access = jwtService.generateToken(user);
        RefreshToken rt = refreshTokenService.create(user);
        return new AuthResponse(access, rt.getToken());
    }

    @Transactional
    public AuthResponse refresh(String refreshToken) {
        RefreshToken rt = refreshTokenService.verify(refreshToken);
        String newAccess = jwtService.generateToken(rt.getUser());
        return new AuthResponse(newAccess, rt.getToken());
    }

    @Transactional
    public void logout(Long userId) {
        refreshTokenService.deleteByUserId(userId);
    }
}