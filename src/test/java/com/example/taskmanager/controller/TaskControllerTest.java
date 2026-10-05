package com.example.taskmanager.controller;

import com.example.taskmanager.dto.TaskRequest;
import com.example.taskmanager.dto.TaskResponse;
import com.example.taskmanager.model.Priority;
import com.example.taskmanager.model.Status;
import com.example.taskmanager.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TaskControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private TaskService taskService;

    private TaskResponse sample() {
        return new TaskResponse(1L, "Test", "desc", Priority.HIGH, Status.NEW, null);
    }

    // ===== AUTH =====

    @Test
    void getAll_whenNotAuthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAll_whenUser_shouldReturn200() throws Exception {
        when(taskService.getAll(null, null)).thenReturn(List.of(sample()));

        mockMvc.perform(get("/api/tasks")
                        .with(user("u@test.com").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Test"));
    }

    @Test
    void getAll_whenAdmin_shouldReturn200() throws Exception {
        when(taskService.getAll(null, null)).thenReturn(List.of(sample()));

        mockMvc.perform(get("/api/tasks")
                        .with(user("a@test.com").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    // ===== GET BY ID =====

    @Test
    void getById_whenExists_shouldReturnTask() throws Exception {
        when(taskService.getById(1L)).thenReturn(sample());

        mockMvc.perform(get("/api/tasks/1")
                        .with(user("u@test.com").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Test"));
    }

    // ===== CREATE =====

    @Test
    void create_withValidBody_shouldReturn201() throws Exception {
        when(taskService.create(any(TaskRequest.class))).thenReturn(sample());

        mockMvc.perform(post("/api/tasks")
                        .with(user("u@test.com").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"New\",\"priority\":\"HIGH\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Test"));
    }

    @Test
    void create_withBlankTitle_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .with(user("u@test.com").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"priority\":\"HIGH\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_withoutPriority_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .with(user("u@test.com").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"X\"}"))
                .andExpect(status().isBadRequest());
    }

    // ===== DELETE =====

    @Test
    void delete_whenUser_shouldReturn403() throws Exception {
        mockMvc.perform(delete("/api/tasks/1")
                        .with(user("u@test.com").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_whenAdmin_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/tasks/1")
                        .with(user("a@test.com").roles("ADMIN")))
                .andExpect(status().isNoContent());
    }

    // ===== STATUS =====

    @Test
    void updateStatus_shouldReturnUpdatedTask() throws Exception {
        TaskResponse done = new TaskResponse(1L, "Test", "d", Priority.HIGH, Status.DONE, null);
        when(taskService.updateStatus(eq(1L), any())).thenReturn(done);

        mockMvc.perform(patch("/api/tasks/1/status")
                        .with(user("u@test.com").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DONE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"));
    }

    // ===== STATS =====

    @Test
    void getStats_shouldReturnMap() throws Exception {
        when(taskService.getStats()).thenReturn(Map.of("NEW", 3L, "IN_PROGRESS", 1L, "DONE", 2L));

        mockMvc.perform(get("/api/tasks/stats")
                        .with(user("u@test.com").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.NEW").value(3))
                .andExpect(jsonPath("$.DONE").value(2));
    }

    // ===== SEARCH =====

    @Test
    void search_shouldReturnList() throws Exception {
        when(taskService.search("milk")).thenReturn(List.of(sample()));

        mockMvc.perform(get("/api/tasks/search?keyword=milk")
                        .with(user("u@test.com").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Test"));
    }

    // ===== MY =====

    @Test
    void getMy_shouldReturnList() throws Exception {
        when(taskService.getMy()).thenReturn(List.of(sample()));

        mockMvc.perform(get("/api/tasks/my")
                        .with(user("u@test.com").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }
}