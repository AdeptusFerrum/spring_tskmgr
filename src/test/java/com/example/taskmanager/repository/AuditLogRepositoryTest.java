package com.example.taskmanager.repository;

import com.example.taskmanager.model.AuditLog;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class AuditLogRepositoryTest {

    @Autowired private AuditLogRepository auditLogRepository;

    private AuditLog build(String action, Long taskId) {
        AuditLog log = new AuditLog();
        log.setAction(action);
        log.setTaskId(taskId);
        log.setDetails("test details");
        log.setCreatedAt(LocalDateTime.now());
        return log;
    }

    @Test
    void save_shouldPersist() {
        AuditLog saved = auditLogRepository.save(build("CREATE", 1L));
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getDetails()).isEqualTo("test details");
    }

    @Test
    void findAll_shouldReturnAllLogs() {
        auditLogRepository.save(build("CREATE", 1L));
        auditLogRepository.save(build("DELETE", 1L));

        List<AuditLog> logs = auditLogRepository.findAll();
        assertThat(logs).hasSize(2);
    }

    @Test
    void save_withoutDetails_shouldWork() {
        AuditLog log = new AuditLog();
        log.setAction("CREATE");
        log.setTaskId(1L);
        log.setCreatedAt(LocalDateTime.now());

        AuditLog saved = auditLogRepository.save(log);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getDetails()).isNull();
    }
}