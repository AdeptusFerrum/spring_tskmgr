package com.example.taskmanager.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AuditLogTest {

    @Test
    void settersAndGetters_shouldWork() {
        AuditLog log = new AuditLog();
        LocalDateTime now = LocalDateTime.now();

        log.setId(1L);
        log.setAction("CREATE");
        log.setTaskId(42L);
        log.setDetails("test");
        log.setCreatedAt(now);

        assertThat(log.getId()).isEqualTo(1L);
        assertThat(log.getAction()).isEqualTo("CREATE");
        assertThat(log.getTaskId()).isEqualTo(42L);
        assertThat(log.getDetails()).isEqualTo("test");
        assertThat(log.getCreatedAt()).isEqualTo(now);
    }
}