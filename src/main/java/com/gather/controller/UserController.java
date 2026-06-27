package com.gather.controller;

import com.gather.exception.InvalidTokenException;
import com.gather.model.dto.request.UpdateLocationRequest;
import com.gather.model.dto.response.LocationResponse;
import com.gather.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PutMapping("/location")
    public ResponseEntity<Map<String, Object>> updateLocation(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody UpdateLocationRequest request) {
        LocationResponse location = userService.updateLocation(extractToken(authHeader), request.getCityId());
        return ResponseEntity.ok(Map.of("location", location));
    }

    @PostMapping("/ensure-profile")
    public ResponseEntity<Void> ensureProfile(@RequestHeader("Authorization") String authHeader) {
        userService.ensureProfile(extractToken(authHeader));
        return ResponseEntity.noContent().build();
    }

    private String extractToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            throw new InvalidTokenException();
        }
        return authHeader.substring(BEARER_PREFIX.length());
    }
}
