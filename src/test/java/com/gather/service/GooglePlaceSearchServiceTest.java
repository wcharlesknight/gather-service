package com.gather.service;

import com.gather.model.domain.Place;
import com.gather.model.provider.google.GooglePlace;
import com.gather.model.provider.google.GooglePlacesSearchResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GooglePlaceSearchServiceTest {

    @Mock GooglePlacesApiService apiService;

    private GooglePlaceSearchService service;

    @BeforeEach
    void setUp() {
        service = new GooglePlaceSearchService(apiService);
    }

    private GooglePlace fullPlace() {
        GooglePlace gp = new GooglePlace();
        gp.setId("place-1");
        GooglePlace.DisplayName dn = new GooglePlace.DisplayName();
        dn.setText("The Pine Box");
        gp.setDisplayName(dn);
        gp.setFormattedAddress("1600 Melrose Ave");
        gp.setRating(4.5);
        gp.setUserRatingCount(120);
        gp.setNationalPhoneNumber("(206) 555-0100");
        gp.setGoogleMapsUri("https://maps.google.com/?cid=1");
        GooglePlace.Location loc = new GooglePlace.Location();
        loc.setLatitude(47.61);
        loc.setLongitude(-122.32);
        gp.setLocation(loc);
        return gp;
    }

    private GooglePlace sparsePlace() {
        // displayName and location null — convertToPlace must not NPE.
        GooglePlace gp = new GooglePlace();
        gp.setId("place-2");
        gp.setFormattedAddress("Unknown");
        return gp;
    }

    @Test
    void convertsPopulatedAndSparsePlaces() {
        GooglePlacesSearchResponse response = new GooglePlacesSearchResponse();
        response.setPlaces(List.of(fullPlace(), sparsePlace()));
        when(apiService.searchPlaces(anyString(), anyInt())).thenReturn(Mono.just(response));

        List<Place> places = service.searchPlaces("Seattle, WA", "bars", 10).block();

        assertThat(places).hasSize(2);

        Place first = places.get(0);
        assertThat(first.getProviderId()).isEqualTo("place-1");
        assertThat(first.getProvider()).isEqualTo("google");
        assertThat(first.getName()).isEqualTo("The Pine Box");
        assertThat(first.getRating()).isEqualTo(4.5);
        assertThat(first.getLatitude()).isEqualTo(47.61);
        assertThat(first.getLongitude()).isEqualTo(-122.32);

        Place second = places.get(1);
        assertThat(second.getProviderId()).isEqualTo("place-2");
        assertThat(second.getName()).isNull();
        assertThat(second.getLatitude()).isNull();
        assertThat(second.getLongitude()).isNull();
    }

    @Test
    void returnsEmptyListWhenResponseHasNoPlaces() {
        GooglePlacesSearchResponse response = new GooglePlacesSearchResponse();
        response.setPlaces(null);
        when(apiService.searchPlaces(anyString(), anyInt())).thenReturn(Mono.just(response));

        List<Place> places = service.searchPlaces("Seattle, WA", "bars", 10).block();

        assertThat(places).isEmpty();
    }
}
