package com.gather.controller;

import com.gather.model.domain.CityJobConfig;
import com.gather.service.CityService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Documents the CURRENT behavior of the city endpoints. Note: write endpoints are
 * presently unauthenticated (audit findings C2/H1) — these tests will be updated when
 * security is added in the hardening wave.
 */
@WebMvcTest(CityController.class)
class CityControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean CityService cityService;

    private CityJobConfig seattle() {
        CityJobConfig c = new CityJobConfig();
        c.setId("seattle");
        c.setCityId("seattle");
        c.setName("Seattle");
        c.setEnabled(true);
        return c;
    }

    @Test
    void getAllReturns200WithCities() throws Exception {
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
    void createReturns200() throws Exception {
        when(cityService.save(any())).thenReturn(seattle());

        mockMvc.perform(post("/api/cities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cityId\":\"seattle\",\"name\":\"Seattle\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cityId").value("seattle"));
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/api/cities/seattle"))
                .andExpect(status().isNoContent());

        verify(cityService).delete(eq("seattle"));
    }
}
