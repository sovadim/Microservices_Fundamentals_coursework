package com.example.resourceprocessor.messaging;

import com.example.resourceprocessor.client.ResourceServiceClient;
import com.example.resourceprocessor.client.SongServiceClient;
import com.example.resourceprocessor.dto.SongMetadataDto;
import com.example.resourceprocessor.service.Mp3MetadataExtractor;
import jakarta.jms.JMSException;
import jakarta.jms.TextMessage;
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
    @Mock
    private ResourceProcessedProducer resourceProcessedProducer;
    @Mock
    private TextMessage textMessage;

    @InjectMocks
    private ResourceUploadConsumer consumer;

    @Test
    void onMessage_validResourceId_fetchesExtractsAndCreatesSong() throws JMSException {
        byte[] mp3 = new byte[]{1, 2, 3};
        SongMetadataDto extracted = buildMetadata();
        when(textMessage.getText()).thenReturn("42");
        when(resourceServiceClient.getResource(42)).thenReturn(mp3);
        when(metadataExtractor.extract(mp3)).thenReturn(extracted);

        consumer.onMessage(textMessage);

        ArgumentCaptor<SongMetadataDto> captor = ArgumentCaptor.forClass(SongMetadataDto.class);
        verify(songServiceClient).createSong(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(42);
        assertThat(captor.getValue().getName()).isEqualTo("We are the champions");
    }

    @Test
    void onMessage_messageWithWhitespace_parsesIdCorrectly() throws JMSException {
        byte[] mp3 = new byte[]{1, 2, 3};
        SongMetadataDto extracted = buildMetadata();
        when(textMessage.getText()).thenReturn("  1  ");
        when(resourceServiceClient.getResource(1)).thenReturn(mp3);
        when(metadataExtractor.extract(mp3)).thenReturn(extracted);

        consumer.onMessage(textMessage);

        verify(resourceServiceClient).getResource(1);
    }

    @Test
    void onMessage_resourceServiceThrows_propagatesException() throws JMSException {
        when(textMessage.getText()).thenReturn("99");
        when(resourceServiceClient.getResource(99)).thenThrow(
                new RuntimeException("Resource service unavailable for resource id=99"));

        assertThatThrownBy(() -> consumer.onMessage(textMessage))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("resource id=99");

        verify(metadataExtractor, never()).extract(any());
        verify(songServiceClient, never()).createSong(any());
    }

    @Test
    void onMessage_extractionThrows_propagatesException() throws JMSException {
        byte[] mp3 = new byte[]{1, 2, 3};
        when(textMessage.getText()).thenReturn("5");
        when(resourceServiceClient.getResource(5)).thenReturn(mp3);
        when(metadataExtractor.extract(mp3)).thenThrow(new RuntimeException("Failed to extract MP3 metadata"));

        assertThatThrownBy(() -> consumer.onMessage(textMessage))
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
