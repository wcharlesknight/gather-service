package com.gather.job;

import com.gather.model.domain.CityJobConfig;
import com.gather.model.domain.GatheringSpot;
import com.gather.model.domain.Place;
import com.gather.service.CityService;
import com.gather.service.EmailService;
import com.gather.service.GatheringSpotService;
import com.gather.service.PlaceSearchService;
import com.gather.service.PushNotificationService;
import com.gather.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GatheringSpotSyncJobTest {

    @Mock PlaceSearchService placeSearchService;
    @Mock PushNotificationService pushNotificationService;
    @Mock CityService cityService;
    @Mock GatheringSpotService gatheringSpotService;
    @Mock UserService userService;
    @Mock EmailService emailService;

    private GatheringSpotSyncJob job;

    @BeforeEach
    void setUp() {
        job = new GatheringSpotSyncJob(placeSearchService, pushNotificationService,
                cityService, gatheringSpotService, userService, emailService);
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
        when(gatheringSpotService.getRecentPlaceIds(eq("seattle"), eq("google"), anyInt()))
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
        when(gatheringSpotService.getRecentPlaceIds(eq("seattle"), eq("google"), anyInt()))
                .thenReturn(List.of("A", "B"));

        // All spots recently used => repeat-avoidance is skipped and a spot is still returned.
        Place selected = job.selectRandomGatheringSpot(List.of(a, b), "seattle");
        assertThat(selected).isIn(a, b);
    }

    @Test
    void proceedsWithFullListWhenRecentLookupFails() {
        Place a = place("A", "Bar A");
        when(placeSearchService.getProviderName()).thenReturn("google");
        when(gatheringSpotService.getRecentPlaceIds(eq("seattle"), eq("google"), anyInt()))
                .thenThrow(new RuntimeException("firestore down"));

        // Exception while checking recent spots is caught; selection continues.
        Place selected = job.selectRandomGatheringSpot(List.of(a), "seattle");
        assertThat(selected).isSameAs(a);
    }

    @Test
    void runsSynchronouslyAndRoutesThroughServices() {
        // @Value fields aren't injected in a plain unit test; enable the job and set sane defaults.
        ReflectionTestUtils.setField(job, "jobEnabled", true);
        ReflectionTestUtils.setField(job, "searchLimit", 20);

        CityJobConfig city = new CityJobConfig();
        city.setCityId("seattle");
        city.setName("Seattle");
        city.setLocation("Seattle, WA");
        city.setSearchTerm("bars");
        city.setTopic("weekly-gather");

        Place spot = place("A", "Bar A");
        when(cityService.getAllEnabled()).thenReturn(List.of(city));
        when(placeSearchService.getProviderName()).thenReturn("google");
        when(placeSearchService.searchPlaces(eq("Seattle, WA"), eq("bars"), anyInt()))
                .thenReturn(Mono.just(List.of(spot)));
        when(gatheringSpotService.getRecentPlaceIds(eq("seattle"), eq("google"), anyInt()))
                .thenReturn(List.of());
        GatheringSpot saved = new GatheringSpot();
        saved.setId("doc-1");
        when(gatheringSpotService.save(any())).thenReturn(saved);
        when(userService.findByCityId("seattle")).thenReturn(List.of());

        job.selectWeeklyGatheringSpot();

        // By the time the (now synchronous) method returns, the spot was saved, the notification
        // flag was set (C3), and users were notified — all via the service layer (M7).
        verify(gatheringSpotService).save(any(GatheringSpot.class));
        verify(gatheringSpotService).markNotificationSent("doc-1");
        verify(pushNotificationService).sendGatheringSpotNotification(eq(spot), eq("weekly-gather"));
    }
}
