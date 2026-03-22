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
        logger.info("Fetching {} recent gathering spots for city: {}", limit, cityId);
        return ResponseEntity.ok(gatheringSpotService.getRecentByCity(cityId, limit));
    }

    @GetMapping("/city/{cityId}/recent-ids")
    public ResponseEntity<List<String>> getRecentPlaceIds(
            @PathVariable String cityId,
            @RequestParam(defaultValue = "12") int weeks,
            @RequestParam(defaultValue = "google") String provider) {
        logger.info("Fetching recent {} place IDs for city: {} (last {} weeks)", provider, cityId, weeks);
        return ResponseEntity.ok(gatheringSpotService.getRecentPlaceIds(cityId, provider, weeks));
    }
}
