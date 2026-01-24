package com.gather.controller;

import com.gather.model.GatheringSpot;
import com.gather.repository.GatheringSpotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/gathering-spots")
public class GatheringSpotController {
    private static final Logger logger = LoggerFactory.getLogger(GatheringSpotController.class);

    private final GatheringSpotRepository gatheringSpotRepository;

    public GatheringSpotController(GatheringSpotRepository gatheringSpotRepository) {
        this.gatheringSpotRepository = gatheringSpotRepository;
    }

    /**
     * Get all gathering spots for a city
     * GET /api/gathering-spots/city/{cityId}
     */
    @GetMapping("/city/{cityId}")
    public ResponseEntity<List<GatheringSpot>> getGatheringSpotsByCity(@PathVariable String cityId) {
        logger.info("Fetching gathering spots for city: {}", cityId);
        List<GatheringSpot> spots = gatheringSpotRepository.findAllByCityId(cityId);
        return ResponseEntity.ok(spots);
    }

    /**
     * Get recent gathering spots for a city
     * GET /api/gathering-spots/city/{cityId}/recent?limit=10
     */
    @GetMapping("/city/{cityId}/recent")
    public ResponseEntity<List<GatheringSpot>> getRecentGatheringSpots(
            @PathVariable String cityId,
            @RequestParam(defaultValue = "10") int limit) {

        logger.info("Fetching {} recent gathering spots for city: {}", limit, cityId);
        List<GatheringSpot> spots = gatheringSpotRepository.findRecentByCityId(cityId, limit);
        return ResponseEntity.ok(spots);
    }

    /**
     * Get recent Yelp IDs for a city (for debugging)
     * GET /api/gathering-spots/city/{cityId}/recent-ids?weeks=12
     */
    @GetMapping("/city/{cityId}/recent-ids")
    public ResponseEntity<List<String>> getRecentYelpIds(
            @PathVariable String cityId,
            @RequestParam(defaultValue = "12") int weeks) {

        logger.info("Fetching recent Yelp IDs for city: {} (last {} weeks)", cityId, weeks);
        List<String> ids = gatheringSpotRepository.findRecentYelpIds(cityId, weeks);
        return ResponseEntity.ok(ids);
    }
}
