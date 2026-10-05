package com.example.taskmanager.controller;

import com.example.taskmanager.dto.TaskRequest;
import com.example.taskmanager.dto.TaskResponse;
import com.example.taskmanager.model.Status;
import com.example.taskmanager.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public List<TaskResponse> getAll(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority) {
        return taskService.getAll(status, priority);
    }

    @GetMapping("/my")
    public List<TaskResponse> getMy() {
        return taskService.getMy();
    }

    @GetMapping("/stats")
    public Map<String, Long> getStats() {
        return taskService.getStats();
    }

    @GetMapping("/{id}")
    public TaskResponse getById(@PathVariable Long id) {
        return taskService.getById(id);
    }

    @PostMapping
    public ResponseEntity<TaskResponse> create(@Valid @RequestBody TaskRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.create(request));
    }

    @PutMapping("/{id}")
    public TaskResponse update(@PathVariable Long id, @Valid @RequestBody TaskRequest request) {
        return taskService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public TaskResponse updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Status status = Status.valueOf(body.get("status"));
        return taskService.updateStatus(id, status);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        taskService.delete(id);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/search")
    public List<TaskResponse> search(@RequestParam String keyword) {
        return taskService.search(keyword);
    }
    @GetMapping("/paged")
    public Page<TaskResponse> getAllPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sort) {
        return taskService.getAllPaged(page, size, sort);
    }
    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportCsv() {
        List<TaskResponse> tasks = taskService.getAll(null, null);
        StringBuilder sb = new StringBuilder();
        sb.append("id,title,description,priority,status\n");
        for (TaskResponse t : tasks) {
            sb.append(t.getId()).append(",")
                    .append(escapeCsv(t.getTitle())).append(",")
                    .append(escapeCsv(t.getDescription())).append(",")
                    .append(t.getPriority()).append(",")
                    .append(t.getStatus()).append("\n");
        }
        byte[] data = sb.toString().getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"tasks.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(data);
    }

    private String escapeCsv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> importCsv(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
        }

        int created = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            boolean firstLine = true;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (firstLine) {
                    firstLine = false;
                    continue; // пропускаем заголовок
                }
                if (line.isBlank()) continue;

                try {
                    String[] parts = parseCsvLine(line);
                    if (parts.length < 2) {
                        errors.add("Line " + lineNumber + ": не хватает полей");
                        failed++;
                        continue;
                    }

                    TaskRequest req = new TaskRequest();
                    req.setTitle(parts[1].trim());
                    req.setDescription(parts.length > 2 ? parts[2].trim() : null);
                    req.setPriority(parts.length > 3 && !parts[3].isBlank()
                            ? com.example.taskmanager.model.Priority.valueOf(parts[3].trim().toUpperCase())
                            : com.example.taskmanager.model.Priority.MEDIUM);

                    taskService.create(req);
                    created++;
                } catch (Exception e) {
                    errors.add("Line " + lineNumber + ": " + e.getMessage());
                    failed++;
                }
            }
        } catch (IOException e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error reading file: " + e.getMessage()));
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("created", created);
        response.put("failed", failed);
        response.put("errors", errors);
        return ResponseEntity.ok(response);
    }

    private String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        result.add(current.toString());
        return result.toArray(new String[0]);
    }
}