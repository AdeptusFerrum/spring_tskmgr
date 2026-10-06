package com.example.taskmanager.controller;

import com.example.taskmanager.dto.TaskRequest;
import com.example.taskmanager.dto.TaskResponse;
import com.example.taskmanager.model.Priority;
import com.example.taskmanager.repository.TagRepository;
import com.example.taskmanager.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/web/tasks")
public class TaskWebController {

    private final TaskService taskService;
    private final TagRepository tagRepository;

    public TaskWebController(TaskService taskService, TagRepository tagRepository) {
        this.taskService = taskService;
        this.tagRepository = tagRepository;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String status,
                       @RequestParam(required = false) String priority,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "5") int size,
                       @RequestParam(defaultValue = "id") String sort,
                       @RequestParam(defaultValue = "asc") String dir,
                       Model model) {

        List<TaskResponse> all = new ArrayList<>(taskService.getAll(status, priority));

        java.util.Comparator<TaskResponse> comparator = switch (sort) {
            case "title" -> java.util.Comparator.comparing(TaskResponse::getTitle, String.CASE_INSENSITIVE_ORDER);
            case "priority" -> java.util.Comparator.comparing(t -> t.getPriority().ordinal());
            case "status" -> java.util.Comparator.comparing(t -> t.getStatus().ordinal());
            default -> java.util.Comparator.comparing(TaskResponse::getId);
        };
        if ("desc".equals(dir)) comparator = comparator.reversed();
        all.sort(comparator);

        int total = all.size();
        int totalPages = (int) Math.ceil((double) total / size);
        int from = Math.min(page * size, total);
        int to = Math.min(from + size, total);

        model.addAttribute("tasks", all.subList(from, to));
        model.addAttribute("stats", taskService.getStats());
        model.addAttribute("currentStatus", status);
        model.addAttribute("currentPriority", priority);
        model.addAttribute("page", page);
        model.addAttribute("size", size);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalElements", total);
        model.addAttribute("hasPrev", page > 0);
        model.addAttribute("hasNext", page < totalPages - 1);
        model.addAttribute("sort", sort);
        model.addAttribute("dir", dir);
        return "tasks/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("task", taskService.getById(id));
        return "tasks/detail";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("taskRequest", new TaskRequest());
        model.addAttribute("priorities", Priority.values());
        model.addAttribute("allTags", tagRepository.findAll());
        return "tasks/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("taskRequest") TaskRequest req,
                         BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("priorities", Priority.values());
            model.addAttribute("allTags", tagRepository.findAll());
            return "tasks/form";
        }
        taskService.create(req);
        return "redirect:/web/tasks";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        TaskResponse task = taskService.getById(id);
        TaskRequest req = new TaskRequest();
        req.setTitle(task.getTitle());
        req.setDescription(task.getDescription());
        req.setPriority(task.getPriority());
        req.setTags(task.getTags() != null ? new ArrayList<>(task.getTags()) : new ArrayList<>());
        model.addAttribute("taskRequest", req);
        model.addAttribute("taskId", id);
        model.addAttribute("priorities", Priority.values());
        model.addAttribute("allTags", tagRepository.findAll());
        return "tasks/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("taskRequest") TaskRequest req,
                         BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("taskId", id);
            model.addAttribute("priorities", Priority.values());
            model.addAttribute("allTags", tagRepository.findAll());
            return "tasks/form";
        }
        taskService.update(id, req);
        return "redirect:/web/tasks";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        taskService.delete(id);
        return "redirect:/web/tasks";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id, @RequestParam String status) {
        taskService.updateStatus(id, com.example.taskmanager.model.Status.valueOf(status));
        return "redirect:/web/tasks";
    }

    @PostMapping("/bulk-delete")
    public String bulkDelete(@RequestParam(value = "ids", required = false) List<Long> ids,
                             RedirectAttributes redirectAttributes) {
        if (ids == null || ids.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Ничего не выбрано");
            return "redirect:/web/tasks";
        }
        int deleted = 0;
        for (Long id : ids) {
            try {
                taskService.delete(id);
                deleted++;
            } catch (Exception ignored) {}
        }
        redirectAttributes.addFlashAttribute("success",
                "Удалено задач: " + deleted + " из " + ids.size());
        return "redirect:/web/tasks";
    }

    @PostMapping("/import")
    public String importCsv(@RequestParam("file") MultipartFile file,
                            RedirectAttributes redirectAttributes) {
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Неверный файл");
            return "redirect:/web/tasks";
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".csv")) {
            redirectAttributes.addFlashAttribute("error", "Неверный файл");
            return "redirect:/web/tasks";
        }

        String contentType = file.getContentType();
        if (contentType != null
                && !contentType.contains("csv")
                && !contentType.contains("text")
                && !contentType.contains("excel")
                && !contentType.contains("octet-stream")) {
            redirectAttributes.addFlashAttribute("error", "Неверный файл");
            return "redirect:/web/tasks";
        }

        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) lines.add(line);
            }
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "Неверный файл");
            return "redirect:/web/tasks";
        }

        if (lines.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Неверный файл");
            return "redirect:/web/tasks";
        }

        String[] header = parseCsvLine(lines.get(0));
        String[] expected = {"id", "title", "description", "priority", "status"};

        if (header.length != expected.length) {
            redirectAttributes.addFlashAttribute("error", "Неверный формат CSV");
            return "redirect:/web/tasks";
        }

        for (int i = 0; i < expected.length; i++) {
            String actual = header[i].trim().replace("\"", "").toLowerCase();
            if (!actual.equals(expected[i])) {
                redirectAttributes.addFlashAttribute("error", "Неверный формат CSV");
                return "redirect:/web/tasks";
            }
        }

        List<TaskRequest> parsed = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            String[] parts = parseCsvLine(lines.get(i));

            if (parts.length != expected.length) {
                redirectAttributes.addFlashAttribute("error", "Неверный формат CSV");
                return "redirect:/web/tasks";
            }

            String title = parts[1].replace("\"", "").trim();
            if (title.isBlank()) {
                redirectAttributes.addFlashAttribute("error", "Неверный формат CSV");
                return "redirect:/web/tasks";
            }

            Priority priority;
            String p = parts[3].replace("\"", "").trim().toUpperCase();
            if (p.isBlank()) {
                priority = Priority.MEDIUM;
            } else if (p.equals("LOW") || p.equals("MEDIUM") || p.equals("HIGH")) {
                priority = Priority.valueOf(p);
            } else {
                redirectAttributes.addFlashAttribute("error", "Неверный формат CSV");
                return "redirect:/web/tasks";
            }

            TaskRequest req = new TaskRequest();
            req.setTitle(title);
            req.setDescription(parts[2].replace("\"", "").trim());
            req.setPriority(priority);
            parsed.add(req);
        }

        int created = 0;
        for (TaskRequest req : parsed) {
            taskService.create(req);
            created++;
        }

        redirectAttributes.addFlashAttribute("success", "Импорт завершён. Создано: " + created);
        return "redirect:/web/tasks";
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