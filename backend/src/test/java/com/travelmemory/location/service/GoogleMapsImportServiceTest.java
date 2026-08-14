package com.travelmemory.location.service;

import com.travelmemory.exception.InvalidGoogleMapsLinkException;
import com.travelmemory.location.dto.GoogleMapsPlaceResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GoogleMapsImportServiceTest {

    private GoogleMapsRedirectResolver redirectResolver;
    private GoogleMapsImportService service;

    @BeforeEach
    void setUp() {
        redirectResolver = mock(GoogleMapsRedirectResolver.class);
        service = new GoogleMapsImportService(redirectResolver);
    }

    @Test
    void importsNameAndCoordinatesFromStandardPlaceUrl() {
        GoogleMapsPlaceResponse place = service.importPlace(
                "https://www.google.com/maps/place/Colosseum/@41.8902,12.4922,17z/data=!3m1!4b1");

        assertThat(place.name()).isEqualTo("Colosseum");
        assertThat(place.latitude()).isEqualByComparingTo("41.8902");
        assertThat(place.longitude()).isEqualByComparingTo("12.4922");
    }

    @Test
    void importsCoordinatesFromGoogleMapsDataParameters() {
        GoogleMapsPlaceResponse place = service.importPlace(
                "https://www.google.com/maps/place/Roman+Forum/data=!8m2!3d41.8925!4d12.4853");

        assertThat(place.name()).isEqualTo("Roman Forum");
        assertThat(place.latitude()).isEqualByComparingTo("41.8925");
        assertThat(place.longitude()).isEqualByComparingTo("12.4853");
    }

    @Test
    void resolvesShortenedLinkBeforeImportingIt() {
        URI shortened = URI.create("https://maps.app.goo.gl/abc123");
        URI resolved = URI.create("https://www.google.com/maps/place/Acropolis/@37.9715,23.7257,17z");
        when(redirectResolver.resolve(shortened)).thenReturn(resolved);

        GoogleMapsPlaceResponse place = service.importPlace(shortened.toString());

        assertThat(place.name()).isEqualTo("Acropolis");
        assertThat(place.latitude()).isEqualByComparingTo("37.9715");
        verify(redirectResolver).resolve(shortened);
    }

    @Test
    void rejectsLinksOutsideGoogleMaps() {
        assertThatThrownBy(() -> service.importPlace("https://example.com/maps?q=41.89,12.49"))
                .isInstanceOf(InvalidGoogleMapsLinkException.class)
                .hasMessageContaining("Only HTTPS Google Maps");
    }

    @Test
    void rejectsGoogleMapsLinkWithoutCoordinates() {
        assertThatThrownBy(() -> service.importPlace("https://www.google.com/maps/place/Colosseum"))
                .isInstanceOf(InvalidGoogleMapsLinkException.class)
                .hasMessageContaining("does not contain place coordinates");
    }
}
