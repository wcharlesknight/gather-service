package com.gather.service;

import com.gather.model.domain.GatheringSpot;
import com.gather.repository.GatheringSpotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GatheringSpotService {
    private static final Logger logger = LoggerFactory.getLogger(GatheringSpotService.class);

    private final GatheringSpotRepository gatheringSpotRepository;

    public GatheringSpotService(GatheringSpotRepository gatheringSpotRepository) {
        this.gatheringSpotRepository = gatheringSpotRepository;
    }

    public List<GatheringSpot> getAllByCity(String cityId) {
        return gatheringSpotRepository.findAllByCityId(cityId);
    }

    public List<GatheringSpot> getRecentByCity(String cityId, int limit) {
        return gatheringSpotRepository.findRecentByCityId(cityId, limit);
    }

    public List<String> getRecentPlaceIds(String cityId, String provider, int weeks) {
        return gatheringSpotRepository.findRecentPlaceIds(cityId, provider, weeks);
    }
}
