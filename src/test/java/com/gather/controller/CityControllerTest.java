package com.gather.controller;

import com.gather.config.security.SecurityConfig;
import com.gather.model.domain.CityJobConfig;
import com.gather.service.CityService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GET endpoints are public; write endpoints require an admin claim (audit findings C2/H1).
 */
@WebMvcTest(CityController.class)
@Import(SecurityConfig.class)
class CityControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean CityService cityService;
    @MockBean FirebaseAuth firebaseAuth;

    @BeforeEach
    void stubTokens() throws Exception {
        FirebaseToken admin = mock(FirebaseToken.class);
        when(admin.getUid()).thenReturn("admin-uid");
        when(admin.getClaims()).thenReturn(Map.of("admin", true));
        when(firebaseAuth.verifyIdToken("admintok", true)).thenReturn(admin);

        FirebaseToken user = mock(FirebaseToken.class);
        when(user.getUid()).thenReturn("user-uid");
        when(user.getClaims()).thenReturn(Map.of());
        when(firebaseAuth.verifyIdToken("usertok", true)).thenReturn(user);
    }

    private CityJobConfig seattle() {
        CityJobConfig c = new CityJobConfig();
        c.setId("seattle");
        c.setCityId("seattle");
        c.setName("Seattle");
        c.setEnabled(true);
        return c;
    }

    @Test
    void getAllReturns200WithoutAuth() throws Exception {
        when(cityService.getAllEnabled()).thenReturn(List.of(seattle()));

        mockMvc.perform(get("/api/cities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cityId").value("seattle"));
    }

    @Test
    void getByIdReturns200WhenFound() throws Exception {
        when(cityService.getById("seattle")).thenReturn(Optional.of(seattle()));

        mockMvc.perform(get("/api/cities/seattle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Seattle"));
    }

    @Test
    void getByIdReturns404WhenMissing() throws Exception {
        when(cityService.getById("nope")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/cities/nope"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createReturns200ForAdmin() throws Exception {
        when(cityService.save(any())).thenReturn(seattle());

        mockMvc.perform(post("/api/cities")
                        .header("Authorization", "Bearer admintok")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cityId\":\"seattle\",\"name\":\"Seattle\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cityId").value("seattle"));
    }

    @Test
    void createReturns401WithoutToken() throws Exception {
        mockMvc.perform(post("/api/cities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cityId\":\"seattle\",\"name\":\"Seattle\"}"))
                .andExpect(status().isUnauthorized());

        verify(cityService, never()).save(any());
    }

    @Test
    void createReturns403ForNonAdmin() throws Exception {
        mockMvc.perform(post("/api/cities")
                        .header("Authorization", "Bearer usertok")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cityId\":\"seattle\",\"name\":\"Seattle\"}"))
                .andExpect(status().isForbidden());

        verify(cityService, never()).save(any());
    }

    @Test
    void deleteReturns204ForAdmin() throws Exception {
        mockMvc.perform(delete("/api/cities/seattle")
                        .header("Authorization", "Bearer admintok"))
                .andExpect(status().isNoContent());

        verify(cityService).delete(eq("seattle"));
    }

    @Test
    void deleteReturns403ForNonAdmin() throws Exception {
        mockMvc.perform(delete("/api/cities/seattle")
                        .header("Authorization", "Bearer usertok"))
                .andExpect(status().isForbidden());

        verify(cityService, never()).delete(any());
    }
}
