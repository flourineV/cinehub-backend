package com.cinehub.profile.controller;

import com.cinehub.profile.service.cloud.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profiles/s3")
@RequiredArgsConstructor
public class S3Controller {

    private final S3Service s3Service;

    @GetMapping("/presigned-url")
    public ResponseEntity<String> getPresignedUrl(
            @RequestParam String fileName,
            @RequestParam String contentType) {

        String url = s3Service.generatePresignedUrl(fileName, contentType);
        return ResponseEntity.ok(url);
    }
}
