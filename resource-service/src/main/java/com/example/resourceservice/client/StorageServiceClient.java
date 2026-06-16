package com.example.resourceservice.client;

import com.example.resourceservice.dto.StorageDto;
import com.example.resourceservice.exception.StorageServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;

@Component
public class StorageServiceClient {

    private final RestTemplate restTemplate;
    private final String storageServiceUrl;

    public StorageServiceClient(RestTemplate restTemplate,
                                @Value("${storage-service.url}") String storageServiceUrl) {
        this.restTemplate = restTemplate;
        this.storageServiceUrl = storageServiceUrl;
    }

    @Retryable(
            retryFor = RestClientException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2.0))
    public StorageDto getStorageByType(String storageType) {
        StorageDto[] storages = restTemplate.getForObject(storageServiceUrl + "/storages", StorageDto[].class);
        if (storages == null || storages.length == 0) {
            throw new StorageServiceException("No storages available");
        }
        return Arrays.stream(storages)
                .filter(s -> storageType.equals(s.getStorageType()))
                .findFirst()
                .orElseThrow(() -> new StorageServiceException("No storage found for type: " + storageType));
    }

    @Recover
    public StorageDto recoverGetStorageByType(RestClientException e, String storageType) {
        throw new StorageServiceException("Storage service unavailable after retries: " + e.getMessage());
    }
}
