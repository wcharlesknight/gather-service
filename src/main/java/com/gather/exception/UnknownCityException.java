package com.gather.exception;

public class UnknownCityException extends RuntimeException {
    public UnknownCityException(String cityId) {
        super("Unknown city: " + cityId);
    }
}
