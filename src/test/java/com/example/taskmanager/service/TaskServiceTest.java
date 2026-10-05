package com.example.taskmanager.service;

import com.example.taskmanager.dto.TaskRequest;
import com.example.taskmanager.dto.TaskResponse;
import com.example.taskmanager.exception.TaskNotFoundException;
import com.example.taskmanager.model.Priority;
import com.example.taskmanager.model.Role;
import com.example.taskmanager.model.Status;
import com.example.taskmanager.model.Task;
import com.example.taskmanager.model.User;
import com.example.taskmanager.repository.TagRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TaskServiceTest {

    @Mock private TaskRepository taskRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditService auditService;
    @Mock private AsyncNotificationService notificationService;
    @Mock private TagRepository tagRepository;

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

    // ========== GET BY ID ==========

    @Test
    void getById_whenExists_shouldReturnTask() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(currentUser));
        when(taskRepository.findById(1L)).thenReturn(Optional.of(buildTask(1L, "Buy milk")));

        TaskResponse result = taskService.getById(1L);

        assertThat(result.getTitle()).isEqualTo("Buy milk");
    }

    @Test
    void getById_whenNotExists_shouldThrow() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(currentUser));
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getById(999L))
                .isInstanceOf(TaskNotFoundException.class)
                .hasMessageContaining("999");
    }

    // ========== GET ALL ==========

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

    // ========== CREATE ==========

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
        verify(auditService).logAction(eq("CREATE"), eq(1L), anyString());
        verify(notificationService).sendTaskCreatedNotification(eq(1L), anyString(), eq("user@test.com"));
    }

    // ========== UPDATE ==========

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

    // ========== UPDATE STATUS ==========

    @Test
    void updateStatus_whenExists_shouldChangeStatus() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(currentUser));
        when(taskRepository.findById(1L)).thenReturn(Optional.of(buildTask(1L, "A")));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(taskService.updateStatus(1L, Status.DONE).getStatus()).isEqualTo(Status.DONE);
    }

    // ========== DELETE ==========

    @Test
    void delete_whenExists_shouldCallRepository() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(currentUser));
        when(taskRepository.findById(1L)).thenReturn(Optional.of(buildTask(1L, "A")));

        taskService.delete(1L);

        verify(taskRepository).deleteById(1L);
        verify(auditService).logAction(eq("DELETE"), eq(1L), anyString());
        verify(notificationService).sendTaskDeletedNotification(1L);
    }

    @Test
    void delete_whenNotExists_shouldThrow() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(currentUser));
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.delete(999L))
                .isInstanceOf(TaskNotFoundException.class);
    }

    // ========== STATS ==========

    @Test
    void getStats_shouldReturnCountByStatus() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(currentUser));
        Task t1 = buildTask(1L, "A"); t1.setStatus(Status.NEW);
        Task t2 = buildTask(2L, "B"); t2.setStatus(Status.NEW);
        Task t3 = buildTask(3L, "C"); t3.setStatus(Status.DONE);
        when(taskRepository.findAllByOwner(currentUser)).thenReturn(List.of(t1, t2, t3));

        var stats = taskService.getStats();

        assertThat(stats.get("NEW")).isEqualTo(2L);
        assertThat(stats.get("IN_PROGRESS")).isEqualTo(0L);
        assertThat(stats.get("DONE")).isEqualTo(1L);
    }
}