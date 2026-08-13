package com.travelmemory.photo.controller;

import com.travelmemory.photo.dto.PhotoResponse;
import com.travelmemory.photo.service.PhotoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/photos")
public class PhotoController {

    private final PhotoService photoService;

    public PhotoController(PhotoService photoService) {
        this.photoService = photoService;
    }

    @GetMapping
    public List<PhotoResponse> list(@PathVariable UUID tripId) {
        return photoService.list(tripId);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public PhotoResponse upload(
            @PathVariable UUID tripId,
            @RequestParam MultipartFile file,
            @RequestParam(required = false) UUID tripStopId,
            @RequestParam(required = false) String caption) {
        return photoService.upload(tripId, tripStopId, caption, file);
    }

    @DeleteMapping("/{photoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID tripId, @PathVariable UUID photoId) {
        photoService.delete(tripId, photoId);
    }
}
