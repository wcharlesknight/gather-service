package com.gather.controller;

import com.gather.model.City;
import com.gather.repository.CityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cities")
public class CityController {
    private static final Logger logger = LoggerFactory.getLogger(CityController.class);

    private final CityRepository cityRepository;

    public CityController(CityRepository cityRepository) {
        this.cityRepository = cityRepository;
    }

    /**
     * Get all enabled cities
     * GET /api/cities
     */
    @GetMapping
    public ResponseEntity<List<City>> getAllCities() {
        logger.info("Fetching all enabled cities");
        List<City> cities = cityRepository.findAllEnabled();
        return ResponseEntity.ok(cities);
    }

    /**
     * Get city by ID
     * GET /api/cities/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<City> getCityById(@PathVariable String id) {
        logger.info("Fetching city with ID: {}", id);
        return cityRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create a new city
     * POST /api/cities
     */
    @PostMapping
    public ResponseEntity<City> createCity(@RequestBody City city) {
        logger.info("Creating new city: {}", city.getName());
        City savedCity = cityRepository.save(city);
        return ResponseEntity.ok(savedCity);
    }

    /**
     * Update a city
     * PUT /api/cities/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<City> updateCity(@PathVariable String id, @RequestBody City city) {
        logger.info("Updating city with ID: {}", id);
        city.setId(id);
        City updatedCity = cityRepository.save(city);
        return ResponseEntity.ok(updatedCity);
    }

    /**
     * Delete a city
     * DELETE /api/cities/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCity(@PathVariable String id) {
        logger.info("Deleting city with ID: {}", id);
        cityRepository.delete(id);
        return ResponseEntity.noContent().build();
    }
}
