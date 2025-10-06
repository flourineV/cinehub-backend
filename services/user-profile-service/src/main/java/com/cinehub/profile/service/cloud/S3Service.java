package com.cinehub.profile.service.cloud;

// Loại bỏ: com.amazonaws...
// Thêm: software.amazon.awssdk...
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Presigner s3Presigner;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    public String generatePresignedUrl(String fileName, String contentType) {

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(5)) // Thời hạn URL (5 phút)
                .putObjectRequest(putObjectRequest)
                .build();

        // Tạo Presigned URL
        PresignedPutObjectRequest presignedPutObjectRequest = s3Presigner.presignPutObject(presignRequest);

        return presignedPutObjectRequest.url().toString();
    }
}