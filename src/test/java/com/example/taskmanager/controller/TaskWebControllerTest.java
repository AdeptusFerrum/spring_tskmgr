package com.example.taskmanager.controller;

import com.example.taskmanager.dto.TaskResponse;
import com.example.taskmanager.model.Priority;
import com.example.taskmanager.model.Status;
import com.example.taskmanager.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TaskWebControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private TaskService taskService;

    private TaskResponse sample() {
        return new TaskResponse(1L, "Web task", "d", Priority.HIGH, Status.NEW, null);
    }

    @Test
    void webTasks_whenNotAuthenticated_shouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/web/tasks"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void webTasks_whenUser_shouldRenderListPage() throws Exception {
        when(taskService.getAll(null, null)).thenReturn(List.of(sample()));
        when(taskService.getStats()).thenReturn(Map.of("NEW", 1L, "IN_PROGRESS", 0L, "DONE", 0L));

        mockMvc.perform(get("/web/tasks")
                        .with(user("u@test.com").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(view().name("tasks/list"))
                .andExpect(model().attributeExists("tasks", "stats"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Web task")));
    }

    @Test
    void webTasksNew_whenUser_shouldRenderForm() throws Exception {
        mockMvc.perform(get("/web/tasks/new")
                        .with(user("u@test.com").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(view().name("tasks/form"))
                .andExpect(model().attributeExists("taskRequest", "priorities"));
    }

    @Test
    void webTasksDetail_whenTaskExists_shouldRenderDetail() throws Exception {
        when(taskService.getById(1L)).thenReturn(sample());

        mockMvc.perform(get("/web/tasks/1")
                        .with(user("u@test.com").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(view().name("tasks/detail"))
                .andExpect(model().attributeExists("task"));
    }
}