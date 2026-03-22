package com.gather.model.provider.google;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GooglePlacesSearchRequest {
    private String textQuery;
    private Integer maxResultCount = 20;
    private String languageCode;
    private String regionCode;

    public GooglePlacesSearchRequest() {
    }

    public GooglePlacesSearchRequest(String textQuery) {
        this.textQuery = textQuery;
    }

    public GooglePlacesSearchRequest(String textQuery, Integer maxResultCount) {
        this.textQuery = textQuery;
        this.maxResultCount = maxResultCount;
    }
}
