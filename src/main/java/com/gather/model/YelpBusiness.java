package com.gather.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class YelpBusiness {
    private String id;
    private String name;
    private String url;
    private String phone;
    private double rating;

    @JsonProperty("review_count")
    private int reviewCount;

    private Location location;
    private Coordinates coordinates;
    private String price;

    public static class Location {
        private String address1;
        private String address2;
        private String address3;
        private String city;
        private String state;

        @JsonProperty("zip_code")
        private String zipCode;

        private String country;

        @JsonProperty("display_address")
        private String[] displayAddress;

        public String getAddress1() {
            return address1;
        }

        public void setAddress1(String address1) {
            this.address1 = address1;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public String[] getDisplayAddress() {
            return displayAddress;
        }

        public void setDisplayAddress(String[] displayAddress) {
            this.displayAddress = displayAddress;
        }

        public String getFormattedAddress() {
            if (displayAddress != null && displayAddress.length > 0) {
                return String.join(", ", displayAddress);
            }
            return address1 + ", " + city + ", " + state;
        }
    }

    public static class Coordinates {
        private double latitude;
        private double longitude;

        public double getLatitude() {
            return latitude;
        }

        public void setLatitude(double latitude) {
            this.latitude = latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        public void setLongitude(double longitude) {
            this.longitude = longitude;
        }
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

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public int getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(int reviewCount) {
        this.reviewCount = reviewCount;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public Coordinates getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(Coordinates coordinates) {
        this.coordinates = coordinates;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }
}
