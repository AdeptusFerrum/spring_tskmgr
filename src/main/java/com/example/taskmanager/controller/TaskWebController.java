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
            redirectAttributes.addFlashAttribute("error", "Файл пустой");
            return "redirect:/web/tasks";
        }

        int created = 0;
        int failed = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            boolean firstLine = true;

            while ((line = reader.readLine()) != null) {
                if (firstLine) { firstLine = false; continue; }
                if (line.isBlank()) continue;

                try {
                    String[] parts = parseCsvLine(line);
                    if (parts.length < 2 || parts[1].isBlank()) {
                        failed++;
                        continue;
                    }

                    TaskRequest req = new TaskRequest();
                    req.setTitle(parts[1].trim());
                    req.setDescription(parts.length > 2 ? parts[2].trim() : null);
                    req.setPriority(parts.length > 3 && !parts[3].isBlank()
                            ? Priority.valueOf(parts[3].trim().toUpperCase())
                            : Priority.MEDIUM);

                    taskService.create(req);
                    created++;
                } catch (Exception e) {
                    failed++;
                }
            }
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка чтения файла: " + e.getMessage());
            return "redirect:/web/tasks";
        }

        redirectAttributes.addFlashAttribute("success",
                "Импорт завершён. Создано: " + created + ", ошибок: " + failed);
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