package com.example.taskmanager.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;

@ExtendWith(MockitoExtension.class)
class AsyncNotificationServiceTest {

    private final AsyncNotificationService service = new AsyncNotificationService();

    @Test
    void sendTaskCreatedNotification_shouldNotThrow() {
        assertThatCode(() -> service.sendTaskCreatedNotification(1L, "Test", "user@test.com"))
                .doesNotThrowAnyException();
    }

    @Test
    void sendTaskDeletedNotification_shouldNotThrow() {
        assertThatCode(() -> service.sendTaskDeletedNotification(1L))
                .doesNotThrowAnyException();
    }
}