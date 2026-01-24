package com.gather.model;

import com.google.cloud.firestore.annotation.DocumentId;

public class GatheringSpot {
    @DocumentId
    private String id;
    private String cityId;
    private String yelpBusinessId;
    private String businessName;
    private String address;
    private Double rating;
    private Long selectedAt;
    private Boolean notificationSent;
    private Long notificationSentAt;
    private String phoneNumber;
    private String yelpUrl;

    public GatheringSpot() {
        // Required for Firestore
    }

    public GatheringSpot(String cityId, YelpBusiness business) {
        this.cityId = cityId;
        this.yelpBusinessId = business.getId();
        this.businessName = business.getName();
        this.address = business.getLocation().getFormattedAddress();
        this.rating = business.getRating();
        this.selectedAt = System.currentTimeMillis();
        this.notificationSent = false;
        this.phoneNumber = business.getPhone();
        this.yelpUrl = business.getUrl();
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

    public String getYelpBusinessId() {
        return yelpBusinessId;
    }

    public void setYelpBusinessId(String yelpBusinessId) {
        this.yelpBusinessId = yelpBusinessId;
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
