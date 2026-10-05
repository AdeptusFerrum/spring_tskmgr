package com.example.taskmanager.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomAuthEntryPointTest {

    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;

    private final CustomAuthEntryPoint entryPoint = new CustomAuthEntryPoint();

    @Test
    void commence_whenApiPath_shouldWriteJson401() throws Exception {
        StringWriter sw = new StringWriter();
        when(request.getRequestURI()).thenReturn("/api/tasks");
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        entryPoint.commence(request, response, null);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).setContentType("application/json");
        assertThat(sw.toString()).contains("Unauthorized").contains("401");
    }

    @Test
    void commence_whenWebPath_shouldRedirectToLogin() throws Exception {
        when(request.getRequestURI()).thenReturn("/web/tasks");

        entryPoint.commence(request, response, null);

        verify(response).sendRedirect("/login");
    }
}