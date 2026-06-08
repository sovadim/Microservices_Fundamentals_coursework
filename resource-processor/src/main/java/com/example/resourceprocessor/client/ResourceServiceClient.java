package com.example.resourceprocessor.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class ResourceServiceClient {

    private final RestTemplate restTemplate;
    private final String resourceServiceUrl;

    public ResourceServiceClient(RestTemplate restTemplate,
                                 @Value("${resource.service.url}") String resourceServiceUrl) {
        this.restTemplate = restTemplate;
        this.resourceServiceUrl = resourceServiceUrl;
    }

    public byte[] getResource(Integer id) {
        return restTemplate.getForObject(resourceServiceUrl + "/resources/{id}", byte[].class, id);
    }
}
