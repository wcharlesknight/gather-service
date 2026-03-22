package com.gather.model.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LocationResponse {
    private String cityId;
    private String cityName;
    private String state;
    private String country;
    private double latitude;
    private double longitude;
}
