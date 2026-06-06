package com.gather.model.domain;

import lombok.Data;

/**
 * Generic place model that abstracts away provider-specific details.
 * Allows easy switching between different place search providers.
 */
@Data
public class Place {
    private String providerId;
    private String provider;
    private String name;
    private String address;
    private Double rating;
    private Integer reviewCount;
    private String phoneNumber;
    private String url;
    private Double latitude;
    private Double longitude;
}
