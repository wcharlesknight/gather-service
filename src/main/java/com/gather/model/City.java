package com.gather.model;

import com.google.cloud.firestore.annotation.DocumentId;

public class City {
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

    public City() {
        // Required for Firestore
    }

    public City(String name, String location, String topic, String cronSchedule,
                String searchTerm, Integer searchLimit, Boolean enabled) {
        this.name = name;
        this.location = location;
        this.topic = topic;
        this.cronSchedule = cronSchedule;
        this.searchTerm = searchTerm;
        this.searchLimit = searchLimit;
        this.enabled = enabled;
        this.createdAt = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getCronSchedule() {
        return cronSchedule;
    }

    public void setCronSchedule(String cronSchedule) {
        this.cronSchedule = cronSchedule;
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
    }

    public Integer getSearchLimit() {
        return searchLimit;
    }

    public void setSearchLimit(Integer searchLimit) {
        this.searchLimit = searchLimit;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }
}
