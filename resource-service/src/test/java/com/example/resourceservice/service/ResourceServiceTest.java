package com.example.resourceservice.service;

import com.example.resourceservice.client.SongServiceClient;
import com.example.resourceservice.dto.DeletedIdsDto;
import com.example.resourceservice.dto.ResourceIdDto;
import com.example.resourceservice.entity.Resource;
import com.example.resourceservice.exception.InvalidMp3Exception;
import com.example.resourceservice.exception.InvalidRequestException;
import com.example.resourceservice.exception.ResourceNotFoundException;
import com.example.resourceservice.messaging.ResourceUploadProducer;
import com.example.resourceservice.repository.ResourceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceServiceTest {

    @Mock
    private ResourceRepository resourceRepository;
    @Mock
    private Mp3MetadataExtractor metadataExtractor;
    @Mock
    private SongServiceClient songServiceClient;
    @Mock
    private S3StorageService s3StorageService;
    @Mock
    private ResourceUploadProducer uploadProducer;

    @InjectMocks
    private ResourceService resourceService;

    @Test
    void upload_validMp3_savesToDbAndS3AndPublishesMessage() {
        byte[] mp3 = new byte[]{1, 2, 3};
        Resource saved = resourceWithId(7);
        when(metadataExtractor.isValidMp3(mp3)).thenReturn(true);
        when(resourceRepository.save(any(Resource.class))).thenReturn(saved);

        ResourceIdDto result = resourceService.upload(mp3);

        assertThat(result.id()).isEqualTo(7);
        verify(s3StorageService).upload(anyString(), eq(mp3));
        verify(uploadProducer).sendResourceId(7);
    }

    @Test
    void upload_nonMp3Bytes_throwsInvalidMp3Exception() {
        byte[] json = "{}".getBytes();
        when(metadataExtractor.isValidMp3(json)).thenReturn(false);

        assertThatThrownBy(() -> resourceService.upload(json))
                .isInstanceOf(InvalidMp3Exception.class)
                .hasMessage("Invalid file format: application/json. Only MP3 files are allowed");

        verify(resourceRepository, never()).save(any());
    }

    @Test
    void upload_emptyData_throwsInvalidMp3Exception() {
        assertThatThrownBy(() -> resourceService.upload(new byte[0]))
                .isInstanceOf(InvalidMp3Exception.class)
                .hasMessage("Request body is empty or missing");
    }

    @Test
    void upload_nullData_throwsInvalidMp3Exception() {
        assertThatThrownBy(() -> resourceService.upload(null))
                .isInstanceOf(InvalidMp3Exception.class)
                .hasMessage("Request body is empty or missing");
    }

    @Test
    void get_existingId_returnsBytesFromS3() {
        byte[] expected = new byte[]{4, 5, 6};
        Resource resource = resourceWithId(3);
        resource.setS3Key("some-key.mp3");
        when(resourceRepository.findById(3)).thenReturn(Optional.of(resource));
        when(s3StorageService.download("some-key.mp3")).thenReturn(expected);

        byte[] result = resourceService.get(3);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void get_nonExistentId_throwsResourceNotFoundException() {
        when(resourceRepository.findById(99999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resourceService.get(99999))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Resource with ID=99999 not found");
    }

    @Test
    void get_zeroId_throwsInvalidRequestException() {
        assertThatThrownBy(() -> resourceService.get(0))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Invalid value '0' for ID. Must be a positive integer");
    }

    @Test
    void get_negativeId_throwsInvalidRequestException() {
        assertThatThrownBy(() -> resourceService.get(-1))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Invalid value '-1' for ID. Must be a positive integer");
    }

    @Test
    void delete_existingId_deletesFromS3DbAndNotifiesSongService() {
        Resource resource = resourceWithId(5);
        resource.setS3Key("key5.mp3");
        when(resourceRepository.findAllById(List.of(5))).thenReturn(List.of(resource));

        DeletedIdsDto result = resourceService.delete("5");

        assertThat(result.ids()).containsExactly(5);
        verify(s3StorageService).delete("key5.mp3");
        verify(resourceRepository).deleteAllById(List.of(5));
        verify(songServiceClient).deleteSongs(List.of(5));
    }

    @Test
    void delete_nonExistentId_returnsEmptyList() {
        when(resourceRepository.findAllById(List.of(99999))).thenReturn(List.of());

        DeletedIdsDto result = resourceService.delete("99999");

        assertThat(result.ids()).isEmpty();
        verify(s3StorageService, never()).delete(anyString());
        verify(songServiceClient).deleteSongs(List.of());
    }

    @Test
    void delete_mixedIds_returnsOnlyExistingAndDeletesOnlyThose() {
        Resource r1 = resourceWithId(5);
        r1.setS3Key("key5.mp3");
        when(resourceRepository.findAllById(List.of(5, 101, 102))).thenReturn(List.of(r1));

        DeletedIdsDto result = resourceService.delete("5,101,102");

        assertThat(result.ids()).containsExactly(5);
        assertThat(result.ids()).doesNotContain(101, 102);
    }

    @Test
    void delete_csvTooLong_throwsInvalidRequestException() {
        String longCsv = "1,".repeat(100) + "1";
        assertThat(longCsv.length()).isGreaterThan(200);

        assertThatThrownBy(() -> resourceService.delete(longCsv))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("CSV string is too long");
    }

    @Test
    void delete_letterInCsv_throwsInvalidRequestException() {
        assertThatThrownBy(() -> resourceService.delete("1,2,3,4,V"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Invalid ID format: 'V'. Only positive integers are allowed");
    }

    @Test
    void delete_negativeIdInCsv_throwsInvalidRequestException() {
        assertThatThrownBy(() -> resourceService.delete("-1"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Invalid ID format: '-1'. Only positive integers are allowed");
    }

    private Resource resourceWithId(int id) {
        var r = new Resource();
        r.setId(id);
        return r;
    }
}