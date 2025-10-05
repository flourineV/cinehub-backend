package com.cinehub.profile.service.cloud;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    public String generatePresignedUrl(String fileName, String contentType) {
        // Thời hạn URL (5 phút)
        Date expiration = new Date(System.currentTimeMillis() + 5 * 60 * 1000);

        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, fileName)
                .withMethod(HttpMethod.PUT)
                .withExpiration(expiration);

        // Quan trọng: set content-type để khi FE upload file ảnh đúng MIME
        request.addRequestParameter("Content-Type", contentType);

        URL url = amazonS3.generatePresignedUrl(request);
        return url.toString();
    }
}
