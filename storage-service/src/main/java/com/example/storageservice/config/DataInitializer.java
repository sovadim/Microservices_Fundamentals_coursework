package com.example.storageservice.config;

import com.example.storageservice.entity.Storage;
import com.example.storageservice.repository.StorageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

@Component
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final StorageRepository storageRepository;
    private final S3Client s3Client;

    @Value("${storage.staging.bucket}")
    private String stagingBucket;

    @Value("${storage.staging.path}")
    private String stagingPath;

    @Value("${storage.permanent.bucket}")
    private String permanentBucket;

    @Value("${storage.permanent.path}")
    private String permanentPath;

    public DataInitializer(StorageRepository storageRepository, S3Client s3Client) {
        this.storageRepository = storageRepository;
        this.s3Client = s3Client;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initialize() {
        seedStorage("STAGING", stagingBucket, stagingPath);
        seedStorage("PERMANENT", permanentBucket, permanentPath);
    }

    private void seedStorage(String type, String bucket, String path) {
        if (!storageRepository.existsByStorageType(type)) {
            Storage storage = new Storage();
            storage.setStorageType(type);
            storage.setBucket(bucket);
            storage.setPath(path);
            storageRepository.save(storage);
            log.info("Seeded {} storage: bucket={}, path={}", type, bucket, path);
        }
        ensureBucketExists(bucket);
    }

    private void ensureBucketExists(String bucket) {
        try {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
            log.info("Created S3 bucket: {}", bucket);
        } catch (BucketAlreadyExistsException | BucketAlreadyOwnedByYouException ignored) {
        }
    }
}
