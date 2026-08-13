package com.travelmemory.photo.service;

import com.travelmemory.exception.PhotoStorageException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

@Service
public class PhotoStorageService {

    private final RestClient restClient;
    private final String supabaseUrl;
    private final String publishableKey;
    private final String bucket;

    public PhotoStorageService(
            @Value("${app.supabase.url}") String supabaseUrl,
            @Value("${app.supabase.publishable-key}") String publishableKey,
            @Value("${app.supabase.photo-bucket}") String bucket) {
        this.restClient = RestClient.create();
        this.supabaseUrl = supabaseUrl.replaceAll("/+$", "");
        this.publishableKey = publishableKey;
        this.bucket = bucket;
    }

    public String bucket() {
        return bucket;
    }

    public void upload(String path, MultipartFile file, String accessToken) {
        try {
            restClient.post()
                    .uri(objectUri(path))
                    .header("apikey", publishableKey)
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .contentType(MediaType.parseMediaType(file.getContentType()))
                    .body(file.getBytes())
                    .retrieve()
                    .toBodilessEntity();
        } catch (IOException exception) {
            throw new PhotoStorageException("The selected image could not be read.");
        } catch (RestClientResponseException exception) {
            throw storageFailure("upload", exception);
        }
    }

    public String createSignedUrl(String path, String accessToken) {
        try {
            SignedUrlPayload payload = restClient.post()
                    .uri(signUri(path))
                    .header("apikey", publishableKey)
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("expiresIn", 3600))
                    .retrieve()
                    .body(SignedUrlPayload.class);
            if (payload == null || payload.signedURL() == null) {
                throw new PhotoStorageException("Supabase did not return a photo access URL.");
            }
            return payload.signedURL().startsWith("http")
                    ? payload.signedURL()
                    : supabaseUrl + "/storage/v1" + payload.signedURL();
        } catch (RestClientResponseException exception) {
            throw storageFailure("open", exception);
        }
    }

    public void delete(String path, String accessToken) {
        try {
            restClient.method(HttpMethod.DELETE)
                    .uri(deleteUri())
                    .header("apikey", publishableKey)
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .body(Map.of("prefixes", List.of(path)))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException exception) {
            throw storageFailure("delete", exception);
        }
    }

    private URI objectUri(String path) {
        return storageUri("object", bucket, path);
    }

    private URI signUri(String path) {
        return storageUri("object", "sign", bucket, path);
    }

    private URI deleteUri() {
        return storageUri("object", bucket);
    }

    private URI storageUri(String... segments) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(supabaseUrl).pathSegment("storage", "v1");
        for (String segment : segments) {
            for (String part : segment.split("/")) {
                builder.pathSegment(part);
            }
        }
        return builder.build().encode().toUri();
    }

    private PhotoStorageException storageFailure(String operation, RestClientResponseException exception) {
        return new PhotoStorageException("Supabase Storage could not %s the photo (HTTP %d)."
                .formatted(operation, exception.getStatusCode().value()));
    }

    private record SignedUrlPayload(String signedURL) {
    }
}
