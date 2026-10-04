package com.example.taskmanager.service;

import com.example.taskmanager.dto.TaskRequest;
import com.example.taskmanager.dto.TaskResponse;
import com.example.taskmanager.exception.TaskNotFoundException;
import com.example.taskmanager.model.Priority;
import com.example.taskmanager.model.Status;
import com.example.taskmanager.model.Task;
import com.example.taskmanager.model.User;
import com.example.taskmanager.repository.TaskRepository;
import com.example.taskmanager.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public TaskService(TaskRepository taskRepository, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    private User currentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Current user not found"));
    }

    public List<TaskResponse> getAll(String status, String priority) {
        User user = currentUser();

        List<Task> tasks;
        if (status != null && priority != null) {
            tasks = taskRepository.findByStatusAndPriority(Status.valueOf(status), Priority.valueOf(priority));
        } else if (status != null) {
            tasks = taskRepository.findByStatus(Status.valueOf(status));
        } else if (priority != null) {
            tasks = taskRepository.findByPriority(Priority.valueOf(priority));
        } else {
            tasks = taskRepository.findAll();
        }

        // USER видит только свои, ADMIN — все
        if (user.getRole().name().equals("ROLE_USER")) {
            tasks = tasks.stream().filter(t -> t.getOwner() != null && t.getOwner().getId().equals(user.getId())).toList();
        }

        return tasks.stream().map(this::toResponse).toList();
    }

    public List<TaskResponse> getMy() {
        User user = currentUser();
        return taskRepository.findAllByOwner(user).stream().map(this::toResponse).toList();
    }

    public TaskResponse getById(Long id) {
        Task task = taskRepository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
        checkOwnership(task);
        return toResponse(task);
    }

    public TaskResponse create(TaskRequest request) {
        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setPriority(request.getPriority());
        task.setStatus(Status.NEW);
        task.setOwner(currentUser());
        return toResponse(taskRepository.save(task));
    }

    public TaskResponse update(Long id, TaskRequest request) {
        Task task = taskRepository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
        checkOwnership(task);
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setPriority(request.getPriority());
        return toResponse(taskRepository.save(task));
    }

    public TaskResponse updateStatus(Long id, Status status) {
        Task task = taskRepository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
        checkOwnership(task);
        task.setStatus(status);
        return toResponse(taskRepository.save(task));
    }

    public void delete(Long id) {
        Task task = taskRepository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
        checkOwnership(task);
        taskRepository.deleteById(id);
    }

    public Map<String, Long> getStats() {
        Map<String, Long> stats = new LinkedHashMap<>();
        for (Status s : Status.values()) {
            stats.put(s.name(), taskRepository.findByStatus(s).stream().count());
        }
        return stats;
    }

    private void checkOwnership(Task task) {
        User user = currentUser();
        if (user.getRole().name().equals("ROLE_ADMIN")) return;
        if (task.getOwner() == null || !task.getOwner().getId().equals(user.getId())) {
            throw new AccessDeniedException("Not your task");
        }
    }

    private TaskResponse toResponse(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getPriority(),
                task.getStatus()
        );
    }
}