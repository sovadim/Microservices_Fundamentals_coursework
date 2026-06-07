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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ResourceService {

    private final ResourceRepository resourceRepository;
    private final Mp3MetadataExtractor metadataExtractor;
    private final SongServiceClient songServiceClient;
    private final S3StorageService s3StorageService;
    private final ResourceUploadProducer uploadProducer;

    public ResourceService(ResourceRepository resourceRepository,
                           Mp3MetadataExtractor metadataExtractor,
                           SongServiceClient songServiceClient,
                           S3StorageService s3StorageService,
                           ResourceUploadProducer uploadProducer) {
        this.resourceRepository = resourceRepository;
        this.metadataExtractor = metadataExtractor;
        this.songServiceClient = songServiceClient;
        this.s3StorageService = s3StorageService;
        this.uploadProducer = uploadProducer;
    }

    @Transactional
    public ResourceIdDto upload(byte[] data) {
        if (data == null || data.length == 0) {
            throw new InvalidMp3Exception("Request body is empty or missing");
        }
        if (!metadataExtractor.isValidMp3(data)) {
            throw new InvalidMp3Exception("Invalid file format: application/json. Only MP3 files are allowed");
        }

        String s3Key = UUID.randomUUID() + ".mp3";
        var resource = new Resource();
        resource.setS3Key(s3Key);
        Resource saved = resourceRepository.save(resource);

        s3StorageService.upload(s3Key, data);
        uploadProducer.sendResourceId(saved.getId());

        return new ResourceIdDto(saved.getId());
    }

    @Transactional(readOnly = true)
    public byte[] get(Integer id) {
        if (id <= 0) {
            throw new InvalidRequestException("Invalid value '" + id + "' for ID. Must be a positive integer");
        }
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource with ID=" + id + " not found"));
        return s3StorageService.download(resource.getS3Key());
    }

    @Transactional
    public DeletedIdsDto delete(String idsCsv) {
        if (idsCsv.length() > 200) {
            throw new InvalidRequestException("CSV string is too long: received " + idsCsv.length() + " characters, maximum allowed is 200");
        }
        List<Integer> ids = parseIds(idsCsv);
        List<Resource> existing = resourceRepository.findAllById(ids);
        List<Integer> existingIds = existing.stream().map(Resource::getId).toList();
        existing.stream().map(Resource::getS3Key).forEach(s3StorageService::delete);
        resourceRepository.deleteAllById(existingIds);
        songServiceClient.deleteSongs(existingIds);
        return new DeletedIdsDto(existingIds);
    }

    private List<Integer> parseIds(String csv) {
        var ids = new ArrayList<Integer>();
        for (String part : csv.split(",")) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                throw new InvalidRequestException("Invalid CSV format: empty ID value");
            }
            try {
                int id = Integer.parseInt(trimmed);
                if (id <= 0) {
                    throw new InvalidRequestException("Invalid ID format: '" + trimmed + "'. Only positive integers are allowed");
                }
                ids.add(id);
            } catch (NumberFormatException e) {
                throw new InvalidRequestException("Invalid ID format: '" + trimmed + "'. Only positive integers are allowed");
            }
        }
        return ids;
    }
}
