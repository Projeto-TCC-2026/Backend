package com.tcc.infrastructure.storage;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Component
public class S3JsonCacheStorage implements JsonCacheStorage {

    private static final Logger log = LoggerFactory.getLogger(
            S3JsonCacheStorage.class);

    private static final String CONTENT_TYPE_JSON = "application/json";

    private final S3Client s3Client;
    private final String bucketName;

    public S3JsonCacheStorage(
            S3Client s3Client,
            @Value("${app.dashboard-cache.s3-bucket}") String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    @Override
    public void write(String key, String json) {
        try {
            // Sem ACL: o bucket é privado e o objeto herda essa política.
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(CONTENT_TYPE_JSON)
                    .build();

            s3Client.putObject(
                    request,
                    RequestBody.fromString(json, StandardCharsets.UTF_8));

            log.info("Cache gravado no S3. key={}", key);

        } catch (Exception e) {
            log.error(
                    "Erro ao gravar cache no S3. key={}, exception={}",
                    key,
                    e.getClass().getSimpleName());
        }
    }

    @Override
    public Optional<String> read(String key) {
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            return Optional.of(
                    s3Client.getObjectAsBytes(request).asUtf8String());

        } catch (NoSuchKeyException e) {
            log.info("Cache não encontrado no S3. key={}", key);
            return Optional.empty();

        } catch (Exception e) {
            log.error(
                    "Erro ao ler cache no S3. key={}, exception={}",
                    key,
                    e.getClass().getSimpleName());
            return Optional.empty();
        }
    }
}
