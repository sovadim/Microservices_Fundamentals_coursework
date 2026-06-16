package com.example.resourceservice.service;

import com.example.resourceservice.client.SongServiceClient;
import com.example.resourceservice.client.StorageServiceClient;
import com.example.resourceservice.dto.DeletedIdsDto;
import com.example.resourceservice.dto.ResourceIdDto;
import com.example.resourceservice.dto.StorageDto;
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
    private final StorageServiceClient storageServiceClient;
    private final S3StorageService s3StorageService;
    private final ResourceUploadProducer uploadProducer;

    public ResourceService(ResourceRepository resourceRepository,
                           Mp3MetadataExtractor metadataExtractor,
                           SongServiceClient songServiceClient,
                           StorageServiceClient storageServiceClient,
                           S3StorageService s3StorageService,
                           ResourceUploadProducer uploadProducer) {
        this.resourceRepository = resourceRepository;
        this.metadataExtractor = metadataExtractor;
        this.songServiceClient = songServiceClient;
        this.storageServiceClient = storageServiceClient;
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

        StorageDto staging = storageServiceClient.getStorageByType("STAGING");
        String uuid = UUID.randomUUID() + ".mp3";
        String s3Key = buildKey(staging.getPath(), uuid);

        var resource = new Resource();
        resource.setS3Key(s3Key);
        resource.setStorageType("STAGING");
        resource.setStorageId(staging.getId());
        resource.setStorageBucket(staging.getBucket());
        Resource saved = resourceRepository.save(resource);

        s3StorageService.upload(staging.getBucket(), s3Key, data);
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
        return s3StorageService.download(resource.getStorageBucket(), resource.getS3Key());
    }

    @Transactional
    public DeletedIdsDto delete(String idsCsv) {
        if (idsCsv.length() > 200) {
            throw new InvalidRequestException("CSV string is too long: received " + idsCsv.length() + " characters, maximum allowed is 200");
        }
        List<Integer> ids = parseIds(idsCsv);
        List<Resource> existing = resourceRepository.findAllById(ids);
        List<Integer> existingIds = existing.stream().map(Resource::getId).toList();
        existing.forEach(r -> s3StorageService.delete(r.getStorageBucket(), r.getS3Key()));
        resourceRepository.deleteAllById(existingIds);
        songServiceClient.deleteSongs(existingIds);
        return new DeletedIdsDto(existingIds);
    }

    @Transactional
    public void moveToPermanent(Integer resourceId) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource with ID=" + resourceId + " not found"));
        if ("PERMANENT".equals(resource.getStorageType())) {
            return;
        }
        StorageDto permanent = storageServiceClient.getStorageByType("PERMANENT");
        String filename = extractFilename(resource.getS3Key());
        String newKey = buildKey(permanent.getPath(), filename);

        s3StorageService.copy(resource.getStorageBucket(), resource.getS3Key(), permanent.getBucket(), newKey);
        s3StorageService.delete(resource.getStorageBucket(), resource.getS3Key());

        resource.setStorageBucket(permanent.getBucket());
        resource.setS3Key(newKey);
        resource.setStorageType("PERMANENT");
        resource.setStorageId(permanent.getId());
    }

    private String buildKey(String path, String filename) {
        String normalizedPath = path.startsWith("/") ? path.substring(1) : path;
        return normalizedPath.isEmpty() ? filename : normalizedPath + "/" + filename;
    }

    private String extractFilename(String key) {
        int slashIdx = key.lastIndexOf('/');
        return slashIdx >= 0 ? key.substring(slashIdx + 1) : key;
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
