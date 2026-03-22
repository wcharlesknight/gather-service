package com.gather.controller;

import com.gather.exception.InvalidTokenException;
import com.gather.exception.UnknownCityException;
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

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PutMapping("/location")
    public ResponseEntity<?> updateLocation(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody UpdateLocationRequest request) {
        String idToken = extractToken(authHeader);
        if (idToken == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Missing or invalid Authorization header."));
        }
        try {
            LocationResponse location = userService.updateLocation(idToken, request.getCityId());
            return ResponseEntity.ok(Map.of("location", location));
        } catch (InvalidTokenException e) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid or expired token."));
        } catch (UnknownCityException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to save location."));
        }
    }

    @PostMapping("/ensure-profile")
    public ResponseEntity<?> ensureProfile(@RequestHeader("Authorization") String authHeader) {
        String idToken = extractToken(authHeader);
        if (idToken == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Missing or invalid Authorization header."));
        }
        try {
            userService.ensureProfile(idToken);
            return ResponseEntity.noContent().build();
        } catch (InvalidTokenException e) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid or expired token."));
        } catch (RuntimeException e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to ensure user profile."));
        }
    }

    private String extractToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        return authHeader.substring(7);
    }
}
