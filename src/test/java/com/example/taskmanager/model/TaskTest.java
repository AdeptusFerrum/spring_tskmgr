package com.example.taskmanager.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TaskTest {

    @Test
    void settersAndGetters_shouldWork() {
        Task t = new Task();
        t.setId(1L);
        t.setTitle("Title");
        t.setDescription("Desc");
        t.setPriority(Priority.HIGH);
        t.setStatus(Status.NEW);

        assertThat(t.getId()).isEqualTo(1L);
        assertThat(t.getTitle()).isEqualTo("Title");
        assertThat(t.getDescription()).isEqualTo("Desc");
        assertThat(t.getPriority()).isEqualTo(Priority.HIGH);
        assertThat(t.getStatus()).isEqualTo(Status.NEW);
    }

    @Test
    void priorityEnum_shouldHaveThreeValues() {
        assertThat(Priority.values()).containsExactly(Priority.LOW, Priority.MEDIUM, Priority.HIGH);
    }

    @Test
    void statusEnum_shouldHaveThreeValues() {
        assertThat(Status.values()).containsExactly(Status.NEW, Status.IN_PROGRESS, Status.DONE);
    }
}