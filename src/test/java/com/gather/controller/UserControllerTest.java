package com.gather.controller;

import com.gather.exception.UnknownCityException;
import com.gather.model.dto.response.LocationResponse;
import com.gather.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean UserService userService;

    @Test
    void updateLocationReturns200() throws Exception {
        when(userService.updateLocation(eq("tok"), eq("seattle")))
                .thenReturn(LocationResponse.builder().cityId("seattle").cityName("Seattle").build());

        mockMvc.perform(put("/api/users/location")
                        .header("Authorization", "Bearer tok")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cityId\":\"seattle\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.location.cityId").value("seattle"));
    }

    @Test
    void updateLocationReturns401WithoutBearerScheme() throws Exception {
        mockMvc.perform(put("/api/users/location")
                        .header("Authorization", "Basic abc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cityId\":\"seattle\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateLocationReturns404ForUnknownCity() throws Exception {
        when(userService.updateLocation(eq("tok"), eq("atlantis")))
                .thenThrow(new UnknownCityException("atlantis"));

        mockMvc.perform(put("/api/users/location")
                        .header("Authorization", "Bearer tok")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cityId\":\"atlantis\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void updateLocationReturns400OnBlankCityId() throws Exception {
        mockMvc.perform(put("/api/users/location")
                        .header("Authorization", "Bearer tok")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
