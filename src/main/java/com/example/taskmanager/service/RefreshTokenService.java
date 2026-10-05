package com.example.taskmanager.service;

import com.example.taskmanager.model.RefreshToken;
import com.example.taskmanager.model.User;
import com.example.taskmanager.repository.RefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private static final long REFRESH_DAYS = 7;

    private final RefreshTokenRepository repo;

    public RefreshTokenService(RefreshTokenRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public RefreshToken create(User user) {
        RefreshToken rt = new RefreshToken();
        rt.setUser(user);
        rt.setToken(UUID.randomUUID().toString());
        rt.setExpiresAt(LocalDateTime.now().plusDays(REFRESH_DAYS));
        return repo.save(rt);
    }

    @Transactional
    public RefreshToken verify(String token) {
        RefreshToken rt = repo.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Refresh token not found"));
        if (rt.getExpiresAt().isBefore(LocalDateTime.now())) {
            repo.delete(rt);
            throw new IllegalArgumentException("Refresh token expired");
        }
        return rt;
    }

    @Transactional
    public void deleteByUserId(Long userId) {
        repo.deleteByUserId(userId);
    }
}