package com.gather.model;

import com.fasterxml.jackson.annotation.JsonInclude;

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

    public String getTextQuery() {
        return textQuery;
    }

    public void setTextQuery(String textQuery) {
        this.textQuery = textQuery;
    }

    public Integer getMaxResultCount() {
        return maxResultCount;
    }

    public void setMaxResultCount(Integer maxResultCount) {
        this.maxResultCount = maxResultCount;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }

    public String getRegionCode() {
        return regionCode;
    }

    public void setRegionCode(String regionCode) {
        this.regionCode = regionCode;
    }
}
