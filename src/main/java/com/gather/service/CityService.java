package com.gather.service;

import com.gather.model.domain.CityJobConfig;
import com.gather.repository.CityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CityService {
    private static final Logger logger = LoggerFactory.getLogger(CityService.class);

    private final CityRepository cityRepository;

    public CityService(CityRepository cityRepository) {
        this.cityRepository = cityRepository;
    }

    public List<CityJobConfig> getAllEnabled() {
        return cityRepository.findAllEnabled();
    }

    public Optional<CityJobConfig> getById(String id) {
        return cityRepository.findById(id);
    }

    public CityJobConfig save(CityJobConfig city) {
        return cityRepository.save(city);
    }

    public void delete(String id) {
        cityRepository.delete(id);
    }
}
