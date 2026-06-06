package com.gather.controller;

import com.gather.job.GatheringSpotSyncJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/jobs")
@ConditionalOnProperty(name = "place-service.admin.enabled", havingValue = "true", matchIfMissing = true)
public class AdminJobController {
    private static final Logger logger = LoggerFactory.getLogger(AdminJobController.class);

    private final GatheringSpotSyncJob gatheringSpotSyncJob;

    public AdminJobController(GatheringSpotSyncJob gatheringSpotSyncJob) {
        this.gatheringSpotSyncJob = gatheringSpotSyncJob;
    }

    @PostMapping("/weekly-gather")
    public ResponseEntity<Map<String, String>> triggerWeeklyGather() {
        logger.info("Manual trigger: weekly gathering spot job");
        gatheringSpotSyncJob.selectWeeklyGatheringSpot();
        return ResponseEntity.accepted().body(Map.of(
                "status", "triggered",
                "job", "weekly-gather"
        ));
    }
}
