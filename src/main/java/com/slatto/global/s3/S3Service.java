package com.slatto.global.s3;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    private static final String S3_URL_FORMAT =
            "https://%s.s3.ap-northeast-2.amazonaws.com/%s";


    public String upload(MultipartFile file) {

        String key = createKey(file);

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(
                    request,
                    RequestBody.fromBytes(file.getBytes())
            );

        } catch (IOException e) {
            throw new RuntimeException("파일 업로드에 실패했습니다.");
        }

        return String.format(
                S3_URL_FORMAT,
                bucket,
                key
        );
    }


    private String createKey(MultipartFile file) {

        String originalFilename = file.getOriginalFilename();

        return "images/"
                + UUID.randomUUID()
                + "-"
                + originalFilename;
    }
}