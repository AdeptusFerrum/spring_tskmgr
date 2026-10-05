package com.example.taskmanager.dto;

import com.example.taskmanager.model.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class TaskRequest {

    @NotBlank(message = "Название не может быть пустым")
    @Size(max = 255, message = "Название не может быть длиннее 255 символов")
    private String title;

    @Size(max = 2000, message = "Описание не может быть длиннее 2000 символов")
    private String description;

    @NotNull(message = "Приоритет обязателен")
    private Priority priority;

    private java.util.List<String> tags;

    public java.util.List<String> getTags() { return tags; }
    public void setTags(java.util.List<String> tags) { this.tags = tags; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }
}