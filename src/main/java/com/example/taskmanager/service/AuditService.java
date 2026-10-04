package com.example.taskmanager.service;

import com.example.taskmanager.model.AuditLog;
import com.example.taskmanager.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(String action, Long taskId) {
        AuditLog log = new AuditLog();
        log.setAction(action);
        log.setTaskId(taskId);
        log.setCreatedAt(LocalDateTime.now());
        auditLogRepository.save(log);
    }
}