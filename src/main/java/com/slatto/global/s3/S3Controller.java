package com.slatto.global.s3;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/s3")
public class S3Controller {

    private final S3Service s3Service;

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public String upload(
            @RequestParam("file") MultipartFile file
    ) {
        return s3Service.upload(file);
    }
}