package com.example.taskmanager.controller;

import com.example.taskmanager.dto.TaskRequest;
import com.example.taskmanager.dto.TaskResponse;
import com.example.taskmanager.model.Priority;
import com.example.taskmanager.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/web/tasks")
public class TaskWebController {

    private final TaskService taskService;

    public TaskWebController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("tasks", taskService.getAll(null, null));
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
        return "tasks/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("taskRequest") TaskRequest req,
                         BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("priorities", Priority.values());
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
        model.addAttribute("taskRequest", req);
        model.addAttribute("taskId", id);
        model.addAttribute("priorities", Priority.values());
        return "tasks/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("taskRequest") TaskRequest req,
                         BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("taskId", id);
            model.addAttribute("priorities", Priority.values());
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
}