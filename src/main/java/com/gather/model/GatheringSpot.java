package com.gather.model;

import com.google.cloud.firestore.annotation.DocumentId;

public class GatheringSpot {
    @DocumentId
    private String id;
    private String cityId;
    private String provider; // "google" or future providers
    private String yelpBusinessId; // Legacy field - kept for backward compatibility with existing Firestore data
    private String googlePlaceId;
    private String businessName;
    private String address;
    private Double rating;
    private Long selectedAt;
    private Boolean notificationSent;
    private Long notificationSentAt;
    private String phoneNumber;
    private String yelpUrl; // Also used for Google Maps URL

    public GatheringSpot() {
        // Required for Firestore
    }

    public GatheringSpot(String cityId, Place place) {
        this.cityId = cityId;
        this.provider = place.getProvider();

        // Set provider-specific ID (currently only Google, but architecture supports future providers)
        if ("google".equals(place.getProvider())) {
            this.googlePlaceId = place.getProviderId();
        }

        this.businessName = place.getName();
        this.address = place.getAddress();
        this.rating = place.getRating();
        this.selectedAt = System.currentTimeMillis();
        this.notificationSent = false;
        this.phoneNumber = place.getPhoneNumber();
        this.yelpUrl = place.getUrl();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCityId() {
        return cityId;
    }

    public void setCityId(String cityId) {
        this.cityId = cityId;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getYelpBusinessId() {
        return yelpBusinessId;
    }

    public void setYelpBusinessId(String yelpBusinessId) {
        this.yelpBusinessId = yelpBusinessId;
    }

    public String getGooglePlaceId() {
        return googlePlaceId;
    }

    public void setGooglePlaceId(String googlePlaceId) {
        this.googlePlaceId = googlePlaceId;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public Long getSelectedAt() {
        return selectedAt;
    }

    public void setSelectedAt(Long selectedAt) {
        this.selectedAt = selectedAt;
    }

    public Boolean getNotificationSent() {
        return notificationSent;
    }

    public void setNotificationSent(Boolean notificationSent) {
        this.notificationSent = notificationSent;
    }

    public Long getNotificationSentAt() {
        return notificationSentAt;
    }

    public void setNotificationSentAt(Long notificationSentAt) {
        this.notificationSentAt = notificationSentAt;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getYelpUrl() {
        return yelpUrl;
    }

    public void setYelpUrl(String yelpUrl) {
        this.yelpUrl = yelpUrl;
    }
}
