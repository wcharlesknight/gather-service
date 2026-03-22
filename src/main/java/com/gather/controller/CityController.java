package com.gather.controller;

import com.gather.model.domain.CityJobConfig;
import com.gather.service.CityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cities")
public class CityController {
    private static final Logger logger = LoggerFactory.getLogger(CityController.class);

    private final CityService cityService;

    public CityController(CityService cityService) {
        this.cityService = cityService;
    }

    @GetMapping
    public ResponseEntity<List<CityJobConfig>> getAllCities() {
        logger.info("Fetching all enabled cities");
        return ResponseEntity.ok(cityService.getAllEnabled());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CityJobConfig> getCityById(@PathVariable String id) {
        logger.info("Fetching city with ID: {}", id);
        return cityService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<CityJobConfig> createCity(@RequestBody CityJobConfig city) {
        logger.info("Creating new city: {}", city.getName());
        return ResponseEntity.ok(cityService.save(city));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CityJobConfig> updateCity(@PathVariable String id, @RequestBody CityJobConfig city) {
        logger.info("Updating city with ID: {}", id);
        city.setId(id);
        return ResponseEntity.ok(cityService.save(city));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCity(@PathVariable String id) {
        logger.info("Deleting city with ID: {}", id);
        cityService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
