package com.example.taskmanager.service;

import com.example.taskmanager.annotation.Loggable;
import com.example.taskmanager.dto.TaskRequest;
import com.example.taskmanager.dto.TaskResponse;
import com.example.taskmanager.exception.TaskNotFoundException;
import com.example.taskmanager.model.Priority;
import com.example.taskmanager.model.Status;
import com.example.taskmanager.model.Tag;
import com.example.taskmanager.model.Task;
import com.example.taskmanager.model.User;
import com.example.taskmanager.repository.TagRepository;
import com.example.taskmanager.repository.TaskRepository;
import com.example.taskmanager.repository.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final AsyncNotificationService notificationService;
    private final TagRepository tagRepository;

    public TaskService(TaskRepository taskRepository,
                       UserRepository userRepository,
                       AuditService auditService,
                       AsyncNotificationService notificationService,
                       TagRepository tagRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.notificationService = notificationService;
        this.tagRepository = tagRepository;
    }

    private User currentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Current user not found"));
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getAll(String status, String priority) {
        User user = currentUser();

        String s = (status == null || status.trim().isEmpty()) ? null : status.trim();
        String p = (priority == null || priority.trim().isEmpty()) ? null : priority.trim();

        List<Task> tasks;
        if (s != null && p != null) {
            tasks = taskRepository.findByStatusAndPriority(Status.valueOf(s), Priority.valueOf(p));
        } else if (s != null) {
            tasks = taskRepository.findByStatus(Status.valueOf(s));
        } else if (p != null) {
            tasks = taskRepository.findByPriority(Priority.valueOf(p));
        } else {
            tasks = taskRepository.findAll();
        }

        if (user.getRole().name().equals("ROLE_USER")) {
            tasks = tasks.stream()
                    .filter(t -> t.getOwner() != null && t.getOwner().getId().equals(user.getId()))
                    .toList();
        }

        return tasks.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getMy() {
        User user = currentUser();
        return taskRepository.findAllByOwner(user).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public TaskResponse getById(Long id) {
        Task task = taskRepository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
        checkOwnership(task);
        return toResponse(task);
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getStats() {
        Map<String, Long> stats = new LinkedHashMap<>();
        for (Status s : Status.values()) {
            stats.put(s.name(), 0L);
        }

        User user = currentUser();

        List<Task> tasks;
        if (user.getRole().name().equals("ROLE_USER")) {
            tasks = taskRepository.findAllByOwner(user);
        } else {
            tasks = taskRepository.findAll();
        }

        for (Task t : tasks) {
            stats.merge(t.getStatus().name(), 1L, Long::sum);
        }

        return stats;
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> search(String keyword) {
        return taskRepository.searchByKeyword(keyword).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> getAllPaged(int page, int size, String sortBy) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
        return taskRepository.findAll(pageable).map(this::toResponse);
    }

    @Loggable
    @CacheEvict(value = "taskStats", allEntries = true)
    public TaskResponse create(TaskRequest request) {
        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setPriority(request.getPriority());
        task.setStatus(Status.NEW);
        User user = currentUser();
        task.setOwner(user);
        applyTags(task, request.getTags());

        Task saved = taskRepository.save(task);
        auditService.logAction("CREATE", saved.getId(),
                "title: '" + saved.getTitle() + "', priority: " + saved.getPriority());
        notificationService.sendTaskCreatedNotification(saved.getId(), saved.getTitle(), user.getEmail());
        return toResponse(saved);
    }

    @CacheEvict(value = "taskStats", allEntries = true)
    public TaskResponse update(Long id, TaskRequest request) {
        Task task = taskRepository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
        checkOwnership(task);

        StringBuilder changes = new StringBuilder();
        if (!task.getTitle().equals(request.getTitle())) {
            changes.append("title: '").append(task.getTitle()).append("' → '").append(request.getTitle()).append("'; ");
        }
        if (task.getDescription() == null
                ? request.getDescription() != null
                : !task.getDescription().equals(request.getDescription())) {
            changes.append("description изменено; ");
        }
        if (task.getPriority() != request.getPriority()) {
            changes.append("priority: ").append(task.getPriority()).append(" → ").append(request.getPriority()).append("; ");
        }

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setPriority(request.getPriority());

        if (request.getTags() != null) {
            task.getTags().clear();
            applyTags(task, request.getTags());
        }

        Task saved = taskRepository.save(task);
        auditService.logAction("UPDATE", saved.getId(),
                changes.length() > 0 ? changes.toString() : "изменений нет");
        return toResponse(saved);
    }

    @CacheEvict(value = "taskStats", allEntries = true)
    public TaskResponse updateStatus(Long id, Status status) {
        Task task = taskRepository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
        checkOwnership(task);

        Status oldStatus = task.getStatus();
        task.setStatus(status);
        Task saved = taskRepository.save(task);

        auditService.logAction("UPDATE_STATUS", id,
                "status: " + oldStatus + " → " + status);
        return toResponse(saved);
    }

    @Loggable
    @CacheEvict(value = "taskStats", allEntries = true)
    public void delete(Long id) {
        Task task = taskRepository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
        checkOwnership(task);
        auditService.logAction("DELETE", id, "title: '" + task.getTitle() + "'");
        taskRepository.deleteById(id);
        notificationService.sendTaskDeletedNotification(id);
    }

    private void applyTags(Task task, List<String> tagNames) {
        if (tagNames == null) return;
        for (String name : tagNames) {
            if (name == null || name.trim().isEmpty()) continue;
            String clean = name.trim();
            Tag tag = tagRepository.findByName(clean)
                    .orElseGet(() -> tagRepository.save(new Tag(clean)));
            task.getTags().add(tag);
        }
    }

    private void checkOwnership(Task task) {
        User user = currentUser();
        if (user.getRole().name().equals("ROLE_ADMIN")) return;
        if (task.getOwner() == null || !task.getOwner().getId().equals(user.getId())) {
            throw new AccessDeniedException("Not your task");
        }
    }

    private TaskResponse toResponse(Task task) {
        Set<String> tagNames = task.getTags().stream()
                .map(Tag::getName)
                .collect(Collectors.toSet());
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getPriority(),
                task.getStatus(),
                tagNames
        );
    }
}