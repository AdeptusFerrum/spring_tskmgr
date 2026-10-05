package com.example.taskmanager.service;

import com.example.taskmanager.model.Status;
import com.example.taskmanager.model.Task;
import com.example.taskmanager.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduledTaskCheckerTest {

    @Mock private TaskRepository taskRepository;

    @InjectMocks private ScheduledTaskChecker checker;

    @Test
    void logTaskStatistics_shouldNotThrow() {
        when(taskRepository.findByStatus(Status.NEW)).thenReturn(List.of(new Task()));
        when(taskRepository.findByStatus(Status.IN_PROGRESS)).thenReturn(List.of());
        when(taskRepository.findByStatus(Status.DONE)).thenReturn(List.of());

        assertThatCode(() -> checker.logTaskStatistics()).doesNotThrowAnyException();
    }

    @Test
    void dailyReport_whenTasksPending_shouldNotThrow() {
        when(taskRepository.findByStatus(Status.NEW)).thenReturn(List.of(new Task(), new Task()));
        when(taskRepository.findByStatus(Status.IN_PROGRESS)).thenReturn(List.of(new Task()));

        assertThatCode(() -> checker.dailyReport()).doesNotThrowAnyException();
    }

    @Test
    void dailyReport_whenAllDone_shouldNotThrow() {
        when(taskRepository.findByStatus(Status.NEW)).thenReturn(List.of());
        when(taskRepository.findByStatus(Status.IN_PROGRESS)).thenReturn(List.of());

        assertThatCode(() -> checker.dailyReport()).doesNotThrowAnyException();
    }
}