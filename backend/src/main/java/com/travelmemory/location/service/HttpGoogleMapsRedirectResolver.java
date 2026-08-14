package com.travelmemory.location.service;

import com.travelmemory.exception.InvalidGoogleMapsLinkException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class HttpGoogleMapsRedirectResolver implements GoogleMapsRedirectResolver {

    private static final int MAX_REDIRECTS = 4;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(4))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    @Override
    public URI resolve(URI shortenedUrl) {
        URI current = shortenedUrl;
        for (int redirect = 0; redirect < MAX_REDIRECTS; redirect++) {
            GoogleMapsLinkSupport.requireSupported(current);
            if (!GoogleMapsLinkSupport.isShortened(current)) return current;

            HttpRequest request = HttpRequest.newBuilder(current)
                    .GET()
                    .timeout(Duration.ofSeconds(6))
                    .header("Accept", "text/html")
                    .header("User-Agent", "TravelMemoryMap/1.0")
                    .build();
            try {
                HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
                if (response.statusCode() < 300 || response.statusCode() >= 400) break;
                String location = response.headers().firstValue("location").orElse(null);
                if (location == null) break;
                current = current.resolve(location);
            } catch (IOException exception) {
                throw new InvalidGoogleMapsLinkException(
                        "The shortened Google Maps link could not be opened. Try pasting the full browser URL instead.");
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new InvalidGoogleMapsLinkException("Google Maps link import was interrupted. Please try again.");
            }
        }
        GoogleMapsLinkSupport.requireSupported(current);
        if (!GoogleMapsLinkSupport.isShortened(current)) return current;
        throw new InvalidGoogleMapsLinkException(
                "The shortened Google Maps link could not be resolved. Try pasting the full browser URL instead.");
    }
}
