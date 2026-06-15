package com.example.resourceprocessor.messaging;

import com.example.resourceprocessor.client.ResourceServiceClient;
import com.example.resourceprocessor.client.SongServiceClient;
import com.example.resourceprocessor.dto.SongMetadataDto;
import com.example.resourceprocessor.service.Mp3MetadataExtractor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceUploadConsumerTest {

    @Mock
    private ResourceServiceClient resourceServiceClient;
    @Mock
    private SongServiceClient songServiceClient;
    @Mock
    private Mp3MetadataExtractor metadataExtractor;

    @InjectMocks
    private ResourceUploadConsumer consumer;

    @Test
    void onMessage_validResourceId_fetchesExtractsAndCreatesSong() {
        byte[] mp3 = new byte[]{1, 2, 3};
        SongMetadataDto extracted = buildMetadata();
        when(resourceServiceClient.getResource(42)).thenReturn(mp3);
        when(metadataExtractor.extract(mp3)).thenReturn(extracted);

        consumer.onMessage("42");

        // verify the resource id is set on the metadata before posting
        ArgumentCaptor<SongMetadataDto> captor = ArgumentCaptor.forClass(SongMetadataDto.class);
        verify(songServiceClient).createSong(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(42);
        assertThat(captor.getValue().getName()).isEqualTo("We are the champions");
    }

    @Test
    void onMessage_messageWithWhitespace_parsesIdCorrectly() {
        byte[] mp3 = new byte[]{1, 2, 3};
        SongMetadataDto extracted = buildMetadata();
        when(resourceServiceClient.getResource(1)).thenReturn(mp3);
        when(metadataExtractor.extract(mp3)).thenReturn(extracted);

        consumer.onMessage("  1  ");

        verify(resourceServiceClient).getResource(1);
    }

    @Test
    void onMessage_resourceServiceThrows_propagatesException() {
        when(resourceServiceClient.getResource(99)).thenThrow(
                new RuntimeException("Resource service unavailable for resource id=99"));

        assertThatThrownBy(() -> consumer.onMessage("99"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("resource id=99");

        verify(metadataExtractor, never()).extract(any());
        verify(songServiceClient, never()).createSong(any());
    }

    @Test
    void onMessage_extractionThrows_propagatesException() {
        byte[] mp3 = new byte[]{1, 2, 3};
        when(resourceServiceClient.getResource(5)).thenReturn(mp3);
        when(metadataExtractor.extract(mp3)).thenThrow(new RuntimeException("Failed to extract MP3 metadata"));

        assertThatThrownBy(() -> consumer.onMessage("5"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to extract MP3 metadata");

        verify(songServiceClient, never()).createSong(any());
    }

    private SongMetadataDto buildMetadata() {
        SongMetadataDto dto = new SongMetadataDto();
        dto.setName("We are the champions");
        dto.setArtist("Queen");
        dto.setAlbum("News of the world");
        dto.setDuration("02:59");
        dto.setYear("1977");
        return dto;
    }
}