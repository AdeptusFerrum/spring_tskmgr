package com.example.taskmanager.service;
import com.example.taskmanager.model.Priority;
import com.example.taskmanager.dto.TaskRequest;
import com.example.taskmanager.dto.TaskResponse;
import com.example.taskmanager.exception.TaskNotFoundException;
import com.example.taskmanager.model.Status;
import com.example.taskmanager.model.Task;
import com.example.taskmanager.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class TaskService {

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public List<TaskResponse> getAll(String status, String priority) {
        List<Task> tasks;

        if (status != null && priority != null) {
            tasks = taskRepository.findByStatusAndPriority(
                    Status.valueOf(status), Priority.valueOf(priority));
        } else if (status != null) {
            tasks = taskRepository.findByStatus(Status.valueOf(status));
        } else if (priority != null) {
            tasks = taskRepository.findByPriority(Priority.valueOf(priority));
        } else {
            tasks = taskRepository.findAll();
        }

        return tasks.stream().map(this::toResponse).toList();
    }
    public Map<String, Long> getStats() {
        Map<String, Long> stats = new java.util.LinkedHashMap<>();
        for (Status s : Status.values()) {
            stats.put(s.name(), taskRepository.findByStatus(s).stream().count());
        }
        return stats;
    }
    public TaskResponse getById(Long id) {
        return toResponse(taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id)));
    }

    public TaskResponse create(TaskRequest request) {
        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setPriority(request.getPriority());
        task.setStatus(Status.NEW);
        return toResponse(taskRepository.save(task));
    }

    public TaskResponse update(Long id, TaskRequest request) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setPriority(request.getPriority());
        return toResponse(taskRepository.save(task));
    }

    public TaskResponse updateStatus(Long id, Status status) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
        task.setStatus(status);
        return toResponse(taskRepository.save(task));
    }

    public void delete(Long id) {
        if (!taskRepository.existsById(id)) {
            throw new TaskNotFoundException(id);
        }
        taskRepository.deleteById(id);
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