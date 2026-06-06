package com.gather.controller;

import com.gather.config.security.SecurityConfig;
import com.gather.job.GatheringSpotSyncJob;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The manual job trigger must be admin-only (audit finding C1 — previously unauthenticated).
 */
@WebMvcTest(AdminJobController.class)
@Import(SecurityConfig.class)
class AdminJobControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean GatheringSpotSyncJob job;
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

    @Test
    void triggerReturns401WithoutToken() throws Exception {
        mockMvc.perform(post("/api/admin/jobs/weekly-gather"))
                .andExpect(status().isUnauthorized());
        verify(job, never()).selectWeeklyGatheringSpot();
    }

    @Test
    void triggerReturns403ForNonAdmin() throws Exception {
        mockMvc.perform(post("/api/admin/jobs/weekly-gather")
                        .header("Authorization", "Bearer usertok"))
                .andExpect(status().isForbidden());
        verify(job, never()).selectWeeklyGatheringSpot();
    }

    @Test
    void triggerReturns202ForAdmin() throws Exception {
        mockMvc.perform(post("/api/admin/jobs/weekly-gather")
                        .header("Authorization", "Bearer admintok"))
                .andExpect(status().isAccepted());
        verify(job).selectWeeklyGatheringSpot();
    }
}
