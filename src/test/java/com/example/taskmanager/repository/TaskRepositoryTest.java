package com.example.taskmanager.repository;

import com.example.taskmanager.model.Priority;
import com.example.taskmanager.model.Status;
import com.example.taskmanager.model.Task;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TaskRepositoryTest {

    @Autowired private TaskRepository taskRepository;

    private Task build(String title, Priority p, Status s) {
        Task t = new Task();
        t.setTitle(title);
        t.setPriority(p);
        t.setStatus(s);
        return t;
    }

    @Test
    void save_shouldPersistTask() {
        Task saved = taskRepository.save(build("Test", Priority.HIGH, Status.NEW));
        assertThat(saved.getId()).isNotNull();
    }

    @Test
    void findByStatus_shouldReturnOnlyMatching() {
        taskRepository.save(build("A", Priority.HIGH, Status.NEW));
        taskRepository.save(build("B", Priority.LOW, Status.DONE));

        assertThat(taskRepository.findByStatus(Status.NEW)).hasSize(1);
        assertThat(taskRepository.findByStatus(Status.DONE)).hasSize(1);
    }

    @Test
    void findByPriority_shouldReturnOnlyMatching() {
        taskRepository.save(build("A", Priority.HIGH, Status.NEW));
        taskRepository.save(build("B", Priority.LOW, Status.NEW));

        assertThat(taskRepository.findByPriority(Priority.HIGH)).hasSize(1);
    }

    @Test
    void findByStatusAndPriority_shouldReturnMatching() {
        taskRepository.save(build("A", Priority.HIGH, Status.NEW));
        taskRepository.save(build("B", Priority.HIGH, Status.DONE));
        taskRepository.save(build("C", Priority.LOW, Status.NEW));

        assertThat(taskRepository.findByStatusAndPriority(Status.NEW, Priority.HIGH)).hasSize(1);
    }
}