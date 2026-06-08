package com.example.resourceprocessor.client;

import com.example.resourceprocessor.dto.SongMetadataDto;
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
public class SongServiceClient {

    private static final Logger log = LoggerFactory.getLogger(SongServiceClient.class);

    private final RestTemplate restTemplate;
    private final String songServiceUrl;

    public SongServiceClient(RestTemplate restTemplate,
                             @Value("${song.service.url}") String songServiceUrl) {
        this.restTemplate = restTemplate;
        this.songServiceUrl = songServiceUrl;
    }

    @Retryable(
            retryFor = {ResourceAccessException.class, HttpServerErrorException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2.0))
    public void createSong(SongMetadataDto metadata) {
        restTemplate.postForEntity(songServiceUrl + "/songs", metadata, Void.class);
    }

    @Recover
    public void recoverCreateSong(Exception e, SongMetadataDto metadata) {
        log.error("Failed to save song metadata (resourceId={}) after all retries", metadata.getId(), e);
        throw new RuntimeException("Song service unavailable for resourceId=" + metadata.getId(), e);
    }
}