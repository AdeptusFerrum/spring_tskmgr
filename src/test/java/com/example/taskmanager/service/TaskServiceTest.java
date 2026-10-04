package com.example.taskmanager.service;

import com.example.taskmanager.dto.TaskRequest;
import com.example.taskmanager.dto.TaskResponse;
import com.example.taskmanager.exception.TaskNotFoundException;
import com.example.taskmanager.model.Priority;
import com.example.taskmanager.model.Role;
import com.example.taskmanager.model.Status;
import com.example.taskmanager.model.Task;
import com.example.taskmanager.model.User;
import com.example.taskmanager.repository.TaskRepository;
import com.example.taskmanager.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TaskServiceTest {

    @Mock private TaskRepository taskRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditService auditService;

    @InjectMocks private TaskService taskService;

    private User currentUser;

    private Task buildTask(Long id, String title) {
        Task t = new Task();
        t.setId(id);
        t.setTitle(title);
        t.setPriority(Priority.HIGH);
        t.setStatus(Status.NEW);
        t.setOwner(currentUser);
        return t;
    }

    private User buildUser(Long id, String email, Role role) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        u.setRole(role);
        u.setPassword("x");
        return u;
    }

    @BeforeEach
    void setup() {
        currentUser = buildUser(1L, "user@test.com", Role.ROLE_USER);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user@test.com", null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getById_whenExists_shouldReturnTask() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(currentUser));
        when(taskRepository.findById(1L)).thenReturn(Optional.of(buildTask(1L, "Buy milk")));

        TaskResponse result = taskService.getById(1L);

        assertThat(result.getTitle()).isEqualTo("Buy milk");
    }

    @Test
    void getById_whenNotExists_shouldThrowTaskNotFound() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(currentUser));
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getById(999L))
                .isInstanceOf(TaskNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    void getAll_whenEmpty_shouldReturnEmptyList() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(currentUser));
        when(taskRepository.findAll()).thenReturn(List.of());

        assertThat(taskService.getAll(null, null)).isEmpty();
    }

    @Test
    void getAll_shouldReturnAllTasks() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(currentUser));
        when(taskRepository.findAll()).thenReturn(List.of(buildTask(1L, "A"), buildTask(2L, "B")));

        assertThat(taskService.getAll(null, null)).hasSize(2);
    }

    @Test
    void create_shouldSaveTaskWithStatusNew() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(currentUser));
        when(taskRepository.save(any(Task.class))).thenReturn(buildTask(1L, "New"));

        TaskRequest req = new TaskRequest();
        req.setTitle("New");
        req.setPriority(Priority.HIGH);

        TaskResponse result = taskService.create(req);

        assertThat(result.getStatus()).isEqualTo(Status.NEW);
        verify(taskRepository).save(any(Task.class));
        verify(auditService).logAction("CREATE", 1L);
    }

    @Test
    void update_whenExists_shouldReturnUpdated() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(currentUser));
        when(taskRepository.findById(1L)).thenReturn(Optional.of(buildTask(1L, "Old")));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskRequest req = new TaskRequest();
        req.setTitle("Updated");
        req.setPriority(Priority.MEDIUM);

        assertThat(taskService.update(1L, req).getTitle()).isEqualTo("Updated");
    }

    @Test
    void update_whenNotExists_shouldThrow() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(currentUser));
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        TaskRequest req = new TaskRequest();
        req.setTitle("X");
        req.setPriority(Priority.LOW);

        assertThatThrownBy(() -> taskService.update(999L, req))
                .isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    void updateStatus_whenExists_shouldChangeStatus() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(currentUser));
        when(taskRepository.findById(1L)).thenReturn(Optional.of(buildTask(1L, "A")));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(taskService.updateStatus(1L, Status.DONE).getStatus()).isEqualTo(Status.DONE);
    }

    @Test
    void delete_whenExists_shouldCallRepository() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(currentUser));
        when(taskRepository.findById(1L)).thenReturn(Optional.of(buildTask(1L, "A")));

        taskService.delete(1L);

        verify(taskRepository).deleteById(1L);
        verify(auditService).logAction("DELETE", 1L);
    }

    @Test
    void delete_whenNotExists_shouldThrow() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(currentUser));
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.delete(999L))
                .isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    void getStats_shouldReturnCountByStatus() {
        when(taskRepository.findByStatus(Status.NEW)).thenReturn(List.of(buildTask(1L, "A"), buildTask(2L, "B")));
        when(taskRepository.findByStatus(Status.IN_PROGRESS)).thenReturn(List.of());
        when(taskRepository.findByStatus(Status.DONE)).thenReturn(List.of(buildTask(3L, "C")));

        var stats = taskService.getStats();

        assertThat(stats.get("NEW")).isEqualTo(2L);
        assertThat(stats.get("DONE")).isEqualTo(1L);
    }
}