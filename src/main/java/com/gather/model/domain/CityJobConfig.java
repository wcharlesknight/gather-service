package com.gather.model.domain;

import com.google.cloud.firestore.annotation.DocumentId;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Firestore document representing per-city configuration for the weekly gathering spot job.
 * Controls search parameters, notification topic, and scheduling per city.
 */
@Data
@NoArgsConstructor
public class CityJobConfig {
    @DocumentId
    private String id;
    private String name;
    private String location;
    private String topic;
    private String cronSchedule;
    private String searchTerm;
    private Integer searchLimit;
    private Boolean enabled;
    private Long createdAt;
}
