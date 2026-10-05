package com.example.taskmanager.controller;

import com.example.taskmanager.dto.*;
import com.example.taskmanager.model.User;
import com.example.taskmanager.service.AuthService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public AuthResponse register(@RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@RequestBody RefreshRequest request) {
        return authService.refresh(request.getRefreshToken());
    }

    @PostMapping("/logout")
    public void logout(@AuthenticationPrincipal User user) {
        if (user != null) {
            authService.logout(user.getId());
        }
    }
}