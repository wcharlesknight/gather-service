package com.gather.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GooglePlacesSearchResponse {
    private List<GooglePlace> places;
    private String nextPageToken;

    public List<GooglePlace> getPlaces() {
        return places;
    }

    public void setPlaces(List<GooglePlace> places) {
        this.places = places;
    }

    public String getNextPageToken() {
        return nextPageToken;
    }

    public void setNextPageToken(String nextPageToken) {
        this.nextPageToken = nextPageToken;
    }
}
