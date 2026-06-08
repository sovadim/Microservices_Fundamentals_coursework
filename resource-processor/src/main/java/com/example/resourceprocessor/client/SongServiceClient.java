package com.example.resourceprocessor.client;

import com.example.resourceprocessor.dto.SongMetadataDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class SongServiceClient {

    private final RestTemplate restTemplate;
    private final String songServiceUrl;

    public SongServiceClient(RestTemplate restTemplate,
                             @Value("${song.service.url}") String songServiceUrl) {
        this.restTemplate = restTemplate;
        this.songServiceUrl = songServiceUrl;
    }

    public void createSong(SongMetadataDto metadata) {
        restTemplate.postForEntity(songServiceUrl + "/songs", metadata, Void.class);
    }
}
