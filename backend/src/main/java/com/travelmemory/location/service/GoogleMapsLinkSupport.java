package com.travelmemory.location.service;

import com.travelmemory.exception.InvalidGoogleMapsLinkException;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class GoogleMapsLinkSupport {

    private static final Pattern AT_COORDINATES = Pattern.compile(
            "/@(-?\\d{1,2}(?:\\.\\d+)?),(-?\\d{1,3}(?:\\.\\d+)?)");
    private static final Pattern DATA_COORDINATES = Pattern.compile(
            "!3d(-?\\d{1,2}(?:\\.\\d+)?)!4d(-?\\d{1,3}(?:\\.\\d+)?)");
    private static final Pattern PLAIN_COORDINATES = Pattern.compile(
            "(-?\\d{1,2}(?:\\.\\d+)?)\\s*,\\s*(-?\\d{1,3}(?:\\.\\d+)?)");
    private static final Pattern PLACE_NAME = Pattern.compile("(?:^|/)place/([^/]+)");

    private GoogleMapsLinkSupport() {
    }

    static URI parseSupportedUri(String value) {
        URI uri;
        try {
            uri = URI.create(value.trim());
        } catch (IllegalArgumentException exception) {
            throw invalid("Paste a valid Google Maps link.");
        }
        requireSupported(uri);
        return uri;
    }

    static void requireSupported(URI uri) {
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        String path = uri.getPath() == null ? "" : uri.getPath();
        boolean secure = "https".equalsIgnoreCase(uri.getScheme());
        boolean standardPort = uri.getPort() == -1 || uri.getPort() == 443;
        boolean googleHost = host.equals("google.com") || host.endsWith(".google.com");
        boolean googleMapsPath = host.equals("maps.google.com") || path.equals("/maps") || path.startsWith("/maps/");
        boolean shortened = host.equals("maps.app.goo.gl")
                || host.equals("goo.gl") && path.startsWith("/maps/");
        if (!secure || !standardPort || uri.getUserInfo() != null || !(shortened || googleHost && googleMapsPath)) {
            throw invalid("Only HTTPS Google Maps place links are supported.");
        }
    }

    static boolean isShortened(URI uri) {
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        return host.equals("maps.app.goo.gl") || host.equals("goo.gl");
    }

    static Coordinates coordinates(URI uri) {
        String decodedUrl = decode(uri.toASCIIString());
        Optional<Coordinates> pathCoordinates = matchCoordinates(AT_COORDINATES, decodedUrl)
                .or(() -> matchCoordinates(DATA_COORDINATES, decodedUrl));
        if (pathCoordinates.isPresent()) return pathCoordinates.get();

        String query = uri.getRawQuery();
        if (query != null) {
            for (String pair : query.split("&")) {
                int separator = pair.indexOf('=');
                String key = decode(separator < 0 ? pair : pair.substring(0, separator));
                if (!key.equals("q") && !key.equals("query") && !key.equals("ll")
                        && !key.equals("destination") && !key.equals("daddr")) continue;
                String value = decode(separator < 0 ? "" : pair.substring(separator + 1));
                Optional<Coordinates> coordinates = matchCoordinates(PLAIN_COORDINATES, value);
                if (coordinates.isPresent()) return coordinates.get();
            }
        }
        throw invalid("This link does not contain place coordinates. Open the place in Google Maps and copy its share link.");
    }

    static String placeName(URI uri) {
        Matcher matcher = PLACE_NAME.matcher(decode(uri.getRawPath() == null ? "" : uri.getRawPath()));
        if (!matcher.find()) return "Imported place";
        String name = matcher.group(1).replace('+', ' ').trim();
        return name.isBlank() ? "Imported place" : name;
    }

    private static Optional<Coordinates> matchCoordinates(Pattern pattern, String value) {
        Matcher matcher = pattern.matcher(value);
        if (!matcher.find()) return Optional.empty();
        BigDecimal latitude = new BigDecimal(matcher.group(1));
        BigDecimal longitude = new BigDecimal(matcher.group(2));
        if (latitude.compareTo(BigDecimal.valueOf(-90)) < 0
                || latitude.compareTo(BigDecimal.valueOf(90)) > 0
                || longitude.compareTo(BigDecimal.valueOf(-180)) < 0
                || longitude.compareTo(BigDecimal.valueOf(180)) > 0) {
            throw invalid("The Google Maps link contains invalid coordinates.");
        }
        return Optional.of(new Coordinates(latitude, longitude));
    }

    private static String decode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            throw invalid("The Google Maps link contains invalid URL encoding.");
        }
    }

    private static InvalidGoogleMapsLinkException invalid(String message) {
        return new InvalidGoogleMapsLinkException(message);
    }

    record Coordinates(BigDecimal latitude, BigDecimal longitude) {
    }
}
