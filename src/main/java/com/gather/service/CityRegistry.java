package com.gather.service;

import com.gather.model.dto.response.LocationResponse;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class CityRegistry {

    private static final Map<String, LocationResponse> CITIES = Map.of(
            "seattle", LocationResponse.builder()
                    .cityId("seattle")
                    .cityName("Seattle")
                    .state("WA")
                    .country("USA")
                    .latitude(47.6062)
                    .longitude(-122.3321)
                    .build(),
            "tacoma", LocationResponse.builder()
                    .cityId("tacoma")
                    .cityName("Tacoma")
                    .state("WA")
                    .country("USA")
                    .latitude(47.2414)
                    .longitude(-122.4594)
                    .build()
    );

    public LocationResponse find(String cityId) {
        return CITIES.get(cityId);
    }
}
