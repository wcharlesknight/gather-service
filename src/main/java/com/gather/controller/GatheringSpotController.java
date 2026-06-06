package com.gather.controller;

import com.gather.model.domain.GatheringSpot;
import com.gather.service.GatheringSpotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/gathering-spots")
public class GatheringSpotController {
    private static final Logger logger = LoggerFactory.getLogger(GatheringSpotController.class);

    private final GatheringSpotService gatheringSpotService;

    public GatheringSpotController(GatheringSpotService gatheringSpotService) {
        this.gatheringSpotService = gatheringSpotService;
    }

    @GetMapping("/city/{cityId}")
    public ResponseEntity<List<GatheringSpot>> getGatheringSpotsByCity(@PathVariable String cityId) {
        logger.info("Fetching gathering spots for city: {}", cityId);
        return ResponseEntity.ok(gatheringSpotService.getAllByCity(cityId));
    }

    @GetMapping("/city/{cityId}/recent")
    public ResponseEntity<List<GatheringSpot>> getRecentGatheringSpots(
            @PathVariable String cityId,
            @RequestParam(defaultValue = "10") int limit) {
        int safeLimit = clamp(limit, 1, MAX_LIMIT);
        logger.info("Fetching {} recent gathering spots for city: {}", safeLimit, cityId);
        return ResponseEntity.ok(gatheringSpotService.getRecentByCity(cityId, safeLimit));
    }

    @GetMapping("/city/{cityId}/recent-ids")
    public ResponseEntity<List<String>> getRecentPlaceIds(
            @PathVariable String cityId,
            @RequestParam(defaultValue = "12") int weeks,
            @RequestParam(defaultValue = "google") String provider) {
        int safeWeeks = clamp(weeks, 1, MAX_WEEKS);
        logger.info("Fetching recent {} place IDs for city: {} (last {} weeks)", provider, cityId, safeWeeks);
        return ResponseEntity.ok(gatheringSpotService.getRecentPlaceIds(cityId, provider, safeWeeks));
    }

    // Bound client-supplied paging params so a single request can't trigger a huge Firestore read.
    private static final int MAX_LIMIT = 50;
    private static final int MAX_WEEKS = 52;

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }
}
