package com.gather.controller;

import com.gather.config.security.SecurityConfig;
import com.gather.exception.UnknownCityException;
import com.gather.model.dto.response.LocationResponse;
import com.gather.service.UserService;
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

import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean UserService userService;
    @MockBean FirebaseAuth firebaseAuth;

    @BeforeEach
    void stubValidToken() throws Exception {
        // "Bearer usertok" verifies to an authenticated (non-admin) user.
        FirebaseToken token = mock(FirebaseToken.class);
        when(token.getUid()).thenReturn("uid123");
        when(token.getClaims()).thenReturn(Map.of());
        when(firebaseAuth.verifyIdToken("usertok", true)).thenReturn(token);
    }

    @Test
    void updateLocationReturns200() throws Exception {
        when(userService.updateLocation(eq("usertok"), eq("seattle")))
                .thenReturn(LocationResponse.builder().cityId("seattle").cityName("Seattle").build());

        mockMvc.perform(put("/api/users/location")
                        .header("Authorization", "Bearer usertok")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cityId\":\"seattle\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.location.cityId").value("seattle"));
    }

    @Test
    void updateLocationReturns401WithoutToken() throws Exception {
        mockMvc.perform(put("/api/users/location")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cityId\":\"seattle\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateLocationReturns401WithNonBearerScheme() throws Exception {
        mockMvc.perform(put("/api/users/location")
                        .header("Authorization", "Basic abc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cityId\":\"seattle\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateLocationReturns404ForUnknownCity() throws Exception {
        when(userService.updateLocation(eq("usertok"), eq("atlantis")))
                .thenThrow(new UnknownCityException("atlantis"));

        mockMvc.perform(put("/api/users/location")
                        .header("Authorization", "Bearer usertok")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cityId\":\"atlantis\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void updateLocationReturns400OnBlankCityId() throws Exception {
        mockMvc.perform(put("/api/users/location")
                        .header("Authorization", "Bearer usertok")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
