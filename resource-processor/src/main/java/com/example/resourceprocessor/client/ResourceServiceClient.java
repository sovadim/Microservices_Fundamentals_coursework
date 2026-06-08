package com.example.resourceprocessor.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Component
public class ResourceServiceClient {

    private static final Logger log = LoggerFactory.getLogger(ResourceServiceClient.class);

    private final RestTemplate restTemplate;
    private final String resourceServiceUrl;

    public ResourceServiceClient(RestTemplate restTemplate,
                                 @Value("${resource.service.url}") String resourceServiceUrl) {
        this.restTemplate = restTemplate;
        this.resourceServiceUrl = resourceServiceUrl;
    }

    @Retryable(
            retryFor = {ResourceAccessException.class, HttpServerErrorException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2.0))
    public byte[] getResource(Integer id) {
        return restTemplate.getForObject(resourceServiceUrl + "/resources/{id}", byte[].class, id);
    }

    @Recover
    public byte[] recoverGetResource(Exception e, Integer id) {
        log.error("Failed to fetch resource {} from resource-service after all retries", id, e);
        throw new RuntimeException("Resource service unavailable for resource id=" + id, e);
    }
}