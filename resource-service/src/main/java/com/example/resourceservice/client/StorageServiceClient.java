package com.example.resourceservice.client;

import com.example.resourceservice.dto.StorageDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;

@Component
public class StorageServiceClient {

    private static final Logger log = LoggerFactory.getLogger(StorageServiceClient.class);

    private final RestTemplate restTemplate;
    private final String storageServiceUrl;
    private final String stagingFallbackBucket;
    private final String stagingFallbackPath;
    private final String permanentFallbackBucket;
    private final String permanentFallbackPath;

    public StorageServiceClient(RestTemplate restTemplate,
                                @Value("${storage-service.url}") String storageServiceUrl,
                                @Value("${storage.fallback.staging.bucket}") String stagingFallbackBucket,
                                @Value("${storage.fallback.staging.path}") String stagingFallbackPath,
                                @Value("${storage.fallback.permanent.bucket}") String permanentFallbackBucket,
                                @Value("${storage.fallback.permanent.path}") String permanentFallbackPath) {
        this.restTemplate = restTemplate;
        this.storageServiceUrl = storageServiceUrl;
        this.stagingFallbackBucket = stagingFallbackBucket;
        this.stagingFallbackPath = stagingFallbackPath;
        this.permanentFallbackBucket = permanentFallbackBucket;
        this.permanentFallbackPath = permanentFallbackPath;
    }

    @CircuitBreaker(name = "storageService", fallbackMethod = "fallbackGetStorageByType")
    public StorageDto getStorageByType(String storageType) {
        StorageDto[] storages = restTemplate.getForObject(storageServiceUrl + "/storages", StorageDto[].class);
        if (storages == null || storages.length == 0) {
            throw new RuntimeException("No storages available from storage-service");
        }
        return Arrays.stream(storages)
                .filter(s -> storageType.equals(s.getStorageType()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No storage found for type: " + storageType));
    }

    public StorageDto fallbackGetStorageByType(String storageType, Throwable ex) {
        log.warn("Circuit breaker fallback for storageType={}, reason: {}", storageType, ex.getMessage());
        if ("STAGING".equals(storageType)) {
            return stubStorage("STAGING", stagingFallbackBucket, stagingFallbackPath);
        }
        return stubStorage("PERMANENT", permanentFallbackBucket, permanentFallbackPath);
    }

    private StorageDto stubStorage(String type, String bucket, String path) {
        StorageDto dto = new StorageDto();
        dto.setStorageType(type);
        dto.setBucket(bucket);
        dto.setPath(path);
        return dto;
    }
}
