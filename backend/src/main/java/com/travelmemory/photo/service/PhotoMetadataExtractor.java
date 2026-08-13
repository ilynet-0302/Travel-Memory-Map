package com.travelmemory.photo.service;

import com.drew.imaging.ImageMetadataReader;
import com.drew.lang.GeoLocation;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.TimeZone;

@Component
public class PhotoMetadataExtractor {

    public ExtractedPhotoMetadata extract(MultipartFile file) {
        try (InputStream input = file.getInputStream()) {
            Metadata metadata = ImageMetadataReader.readMetadata(input);
            return new ExtractedPhotoMetadata(extractTakenAt(metadata), extractLatitude(metadata), extractLongitude(metadata));
        } catch (Exception ignored) {
            // EXIF is optional. A valid image without metadata must still be uploadable.
            return ExtractedPhotoMetadata.empty();
        }
    }

    private OffsetDateTime extractTakenAt(Metadata metadata) {
        ExifSubIFDDirectory directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
        if (directory == null) return null;
        Date date = directory.getDateOriginal(TimeZone.getTimeZone("UTC"));
        return date == null ? null : OffsetDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC);
    }

    private BigDecimal extractLatitude(Metadata metadata) {
        GeoLocation location = extractLocation(metadata);
        return location == null ? null : BigDecimal.valueOf(location.getLatitude());
    }

    private BigDecimal extractLongitude(Metadata metadata) {
        GeoLocation location = extractLocation(metadata);
        return location == null ? null : BigDecimal.valueOf(location.getLongitude());
    }

    private GeoLocation extractLocation(Metadata metadata) {
        GpsDirectory directory = metadata.getFirstDirectoryOfType(GpsDirectory.class);
        GeoLocation location = directory == null ? null : directory.getGeoLocation();
        return location == null || location.isZero() ? null : location;
    }
}
