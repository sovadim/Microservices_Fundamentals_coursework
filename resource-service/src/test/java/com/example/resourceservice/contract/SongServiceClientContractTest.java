package com.example.resourceservice.contract;

import com.example.resourceservice.client.SongServiceClient;
import com.example.resourceservice.dto.SongMetadataDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;
import software.amazon.awssdk.services.s3.S3Client;

import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:contractdb;DB_CLOSE_DELAY=-1")
@AutoConfigureStubRunner(
        ids = "com.example:song-service:0.0.1-SNAPSHOT:stubs:8099",
        stubsMode = StubRunnerProperties.StubsMode.CLASSPATH)
class SongServiceClientContractTest {

    @MockBean
    private S3Client s3Client;

    @Autowired
    private SongServiceClient songServiceClient;

    @TestConfiguration
    static class Config {
        @Bean
        @Primary
        public RestTemplate plainRestTemplate() {
            return new RestTemplate();
        }
    }

    @Test
    void createSong_postsToSongService() {
        SongMetadataDto dto = new SongMetadataDto();
        dto.setId(42);
        dto.setName("We are the champions");
        dto.setArtist("Queen");
        dto.setAlbum("News of the world");
        dto.setDuration("02:59");
        dto.setYear("1977");

        songServiceClient.createSong(dto);
    }

    @Test
    void deleteSongs_sendsDeleteToSongService() {
        songServiceClient.deleteSongs(List.of(5));
    }
}