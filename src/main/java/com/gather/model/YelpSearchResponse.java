package com.gather.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class YelpSearchResponse {
    private List<YelpBusiness> businesses;
    private int total;

    public List<YelpBusiness> getBusinesses() {
        return businesses;
    }

    public void setBusinesses(List<YelpBusiness> businesses) {
        this.businesses = businesses;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }
}
