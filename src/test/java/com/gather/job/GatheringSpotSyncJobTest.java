package com.gather.job;

import com.gather.model.domain.Place;
import com.gather.repository.CityRepository;
import com.gather.repository.GatheringSpotRepository;
import com.gather.repository.UserRepository;
import com.gather.service.EmailService;
import com.gather.service.PlaceSearchService;
import com.gather.service.PushNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GatheringSpotSyncJobTest {

    @Mock PlaceSearchService placeSearchService;
    @Mock PushNotificationService pushNotificationService;
    @Mock CityRepository cityRepository;
    @Mock GatheringSpotRepository gatheringSpotRepository;
    @Mock UserRepository userRepository;
    @Mock EmailService emailService;

    private GatheringSpotSyncJob job;

    @BeforeEach
    void setUp() {
        job = new GatheringSpotSyncJob(placeSearchService, pushNotificationService,
                cityRepository, gatheringSpotRepository, userRepository, emailService);
    }

    private Place place(String providerId, String name) {
        Place p = new Place();
        p.setProviderId(providerId);
        p.setProvider("google");
        p.setName(name);
        return p;
    }

    @Test
    void returnsNullForEmptyList() {
        assertThat(job.selectRandomGatheringSpot(List.of(), "seattle")).isNull();
    }

    @Test
    void returnsNullForNullList() {
        assertThat(job.selectRandomGatheringSpot(null, "seattle")).isNull();
    }

    @Test
    void picksFromListWhenCityIdIsNull() {
        Place a = place("A", "Bar A");
        // cityId == null => no repeat-avoidance lookup, just a random pick from the list
        Place selected = job.selectRandomGatheringSpot(List.of(a), null);
        assertThat(selected).isSameAs(a);
    }

    @Test
    void excludesRecentlySelectedSpots() {
        Place a = place("A", "Bar A");
        Place b = place("B", "Bar B");
        Place c = place("C", "Bar C");
        when(placeSearchService.getProviderName()).thenReturn("google");
        when(gatheringSpotRepository.findRecentPlaceIds(eq("seattle"), eq("google"), anyInt()))
                .thenReturn(List.of("A", "B"));

        // Only C is not recently used, so it must be the one picked.
        Place selected = job.selectRandomGatheringSpot(List.of(a, b, c), "seattle");
        assertThat(selected).isSameAs(c);
    }

    @Test
    void fallsBackToFullListWhenAllRecentlyUsed() {
        Place a = place("A", "Bar A");
        Place b = place("B", "Bar B");
        when(placeSearchService.getProviderName()).thenReturn("google");
        when(gatheringSpotRepository.findRecentPlaceIds(eq("seattle"), eq("google"), anyInt()))
                .thenReturn(List.of("A", "B"));

        // All spots recently used => repeat-avoidance is skipped and a spot is still returned.
        Place selected = job.selectRandomGatheringSpot(List.of(a, b), "seattle");
        assertThat(selected).isIn(a, b);
    }

    @Test
    void proceedsWithFullListWhenRecentLookupFails() {
        Place a = place("A", "Bar A");
        when(placeSearchService.getProviderName()).thenReturn("google");
        when(gatheringSpotRepository.findRecentPlaceIds(eq("seattle"), eq("google"), anyInt()))
                .thenThrow(new RuntimeException("firestore down"));

        // Exception while checking recent spots is caught; selection continues.
        Place selected = job.selectRandomGatheringSpot(List.of(a), "seattle");
        assertThat(selected).isSameAs(a);
    }
}
