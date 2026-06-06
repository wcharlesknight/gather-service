package com.gather.controller;

import com.gather.model.dto.response.AuthResponse;
import com.gather.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean AuthService authService;

    @Test
    void registerReturns201WithToken() throws Exception {
        when(authService.register(any())).thenReturn(AuthResponse.builder()
                .uid("uid123").customToken("tok").displayName("Alice").email("alice@example.com").build());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"alice@example.com\",\"password\":\"hunter2\",\"displayName\":\"Alice\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.uid").value("uid123"))
                .andExpect(jsonPath("$.customToken").value("tok"));
    }

    @Test
    void registerReturns409WhenEmailExists() throws Exception {
        when(authService.register(any())).thenThrow(new AuthService.EmailAlreadyExistsException());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"alice@example.com\",\"password\":\"hunter2\",\"displayName\":\"Alice\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void registerReturns400OnInvalidBody() throws Exception {
        // blank email + short password + blank displayName all violate bean validation
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"\",\"password\":\"123\",\"displayName\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginReturns200WithBearerToken() throws Exception {
        when(authService.login(eq("tok"))).thenReturn(AuthResponse.builder().uid("uid123").build());

        mockMvc.perform(post("/api/auth/login").header("Authorization", "Bearer tok"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uid").value("uid123"));
    }

    @Test
    void loginReturns401WithoutBearerScheme() throws Exception {
        mockMvc.perform(post("/api/auth/login").header("Authorization", "Basic abc"))
                .andExpect(status().isUnauthorized());
    }
}
