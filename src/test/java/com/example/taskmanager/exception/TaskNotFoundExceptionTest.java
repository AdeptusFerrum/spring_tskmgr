package com.example.taskmanager.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TaskNotFoundExceptionTest {

    @Test
    void message_shouldContainId() {
        TaskNotFoundException ex = new TaskNotFoundException(42L);
        assertThat(ex.getMessage()).contains("42");
    }
}