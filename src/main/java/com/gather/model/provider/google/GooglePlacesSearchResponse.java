package com.gather.model.provider.google;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GooglePlacesSearchResponse {
    private List<GooglePlace> places;
    private String nextPageToken;
}
