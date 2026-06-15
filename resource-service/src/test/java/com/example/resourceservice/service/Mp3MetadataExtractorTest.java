package com.example.resourceservice.service;

import com.example.resourceservice.dto.SongMetadataDto;
import com.example.resourceservice.exception.InvalidMp3Exception;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Mp3MetadataExtractorTest {

    private final Mp3MetadataExtractor extractor = new Mp3MetadataExtractor();

    @Test
    void isValidMp3_realMp3Bytes_returnsTrue() throws IOException {
        byte[] mp3 = loadTestMp3();
        assertThat(extractor.isValidMp3(mp3)).isTrue();
    }

    @Test
    void isValidMp3_textBytes_returnsFalse() {
        byte[] text = "{ \"not\": \"an mp3\" }".getBytes();
        assertThat(extractor.isValidMp3(text)).isFalse();
    }

    @Test
    void isValidMp3_emptyBytes_returnsFalse() {
        assertThat(extractor.isValidMp3(new byte[0])).isFalse();
    }

    @Test
    void extract_validMp3WithTags_returnsPopulatedDto() throws IOException {
        byte[] mp3 = loadTestMp3();

        SongMetadataDto dto = extractor.extract(mp3);

        assertThat(dto).isNotNull();
        assertThat(dto.getName()).isNotEmpty();
        assertThat(dto.getArtist()).isNotEmpty();
        assertThat(dto.getAlbum()).isNotEmpty();
        // duration must match mm:ss pattern
        assertThat(dto.getDuration()).matches("\\d{2}:\\d{2}");
        // year must be 4-digit between 1900 and 2099
        assertThat(dto.getYear()).matches("(19|20)\\d{2}");
    }

    @Test
    void extract_nonMp3Bytes_throwsInvalidMp3Exception() {
        byte[] garbage = new byte[]{0x00, 0x01, 0x02, 0x03};

        assertThatThrownBy(() -> extractor.extract(garbage))
                .isInstanceOf(InvalidMp3Exception.class)
                .hasMessageContaining("Failed to extract MP3 metadata");
    }

    @Test
    void extract_durationFormattedAsMinutesAndSeconds() throws IOException {
        byte[] mp3 = loadTestMp3();

        SongMetadataDto dto = extractor.extract(mp3);

        // mm:ss — minutes part is always 2 digits, seconds 00-59
        String[] parts = dto.getDuration().split(":");
        assertThat(parts).hasSize(2);
        assertThat(Integer.parseInt(parts[0])).isGreaterThanOrEqualTo(0);
        assertThat(Integer.parseInt(parts[1])).isBetween(0, 59);
    }

    private byte[] loadTestMp3() throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/test-files/valid-sample.mp3")) {
            assertThat(is).as("Test MP3 fixture not found on classpath").isNotNull();
            return is.readAllBytes();
        }
    }
}