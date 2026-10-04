package com.example.taskmanager;

import com.example.taskmanager.dto.AuthResponse;
import com.example.taskmanager.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class TaskApiIntegrationTest {

    @Autowired
    private RestTestClient rest;

    private String registerAndGetToken(String email) {
        RegisterRequest r = new RegisterRequest();
        r.setEmail(email);
        r.setPassword("pass123");
        AuthResponse resp = rest.post().uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(r)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AuthResponse.class)
                .returnResult().getResponseBody();
        return resp.getToken();
    }

    @Test
    void getAll_withoutToken_shouldReturn401() {
        rest.get().uri("/api/tasks")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void getAll_withUserToken_shouldReturn200() {
        String token = registerAndGetToken("user" + System.nanoTime() + "@test.com");
        rest.get().uri("/api/tasks")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void delete_withUserToken_shouldReturn403() {
        String token = registerAndGetToken("user" + System.nanoTime() + "@test.com");
        rest.delete().uri("/api/tasks/1")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void register_shouldReturnToken() {
        String email = "reg" + System.nanoTime() + "@test.com";
        RegisterRequest r = new RegisterRequest();
        r.setEmail(email);
        r.setPassword("pass123");
        AuthResponse resp = rest.post().uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(r)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AuthResponse.class)
                .returnResult().getResponseBody();
        assertThat(resp.getToken()).isNotBlank();
    }
}