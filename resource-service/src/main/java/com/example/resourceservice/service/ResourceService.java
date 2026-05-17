package com.example.resourceservice.service;

import com.example.resourceservice.client.SongServiceClient;
import com.example.resourceservice.dto.DeletedIdsDto;
import com.example.resourceservice.dto.ResourceIdDto;
import com.example.resourceservice.dto.SongMetadataDto;
import com.example.resourceservice.entity.Resource;
import com.example.resourceservice.exception.InvalidMp3Exception;
import com.example.resourceservice.exception.InvalidRequestException;
import com.example.resourceservice.exception.ResourceNotFoundException;
import com.example.resourceservice.repository.ResourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ResourceService {

    private final ResourceRepository resourceRepository;
    private final Mp3MetadataExtractor metadataExtractor;
    private final SongServiceClient songServiceClient;

    public ResourceService(ResourceRepository resourceRepository,
                           Mp3MetadataExtractor metadataExtractor,
                           SongServiceClient songServiceClient) {
        this.resourceRepository = resourceRepository;
        this.metadataExtractor = metadataExtractor;
        this.songServiceClient = songServiceClient;
    }

    @Transactional
    public ResourceIdDto upload(byte[] data) {
        if (data == null || data.length == 0) {
            throw new InvalidMp3Exception("Request body is empty or missing");
        }
        if (!metadataExtractor.isValidMp3(data)) {
            throw new InvalidMp3Exception("Invalid file format: application/json. Only MP3 files are allowed");
        }

        SongMetadataDto songMetadata = metadataExtractor.extract(data);

        var resource = new Resource();
        resource.setData(data);
        Resource saved = resourceRepository.save(resource);

        songMetadata.setId(saved.getId());
        songServiceClient.createSong(songMetadata);

        return new ResourceIdDto(saved.getId());
    }

    @Transactional(readOnly = true)
    public byte[] get(Integer id) {
        if (id <= 0) {
            throw new InvalidRequestException("Invalid value '" + id + "' for ID. Must be a positive integer");
        }
        return resourceRepository.findById(id)
                .map(Resource::getData)
                .orElseThrow(() -> new ResourceNotFoundException("Resource with ID=" + id + " not found"));
    }

    @Transactional
    public DeletedIdsDto delete(String idsCsv) {
        if (idsCsv.length() > 200) {
            throw new InvalidRequestException("CSV string is too long: received " + idsCsv.length() + " characters, maximum allowed is 200");
        }
        List<Integer> ids = parseIds(idsCsv);
        List<Integer> existingIds = resourceRepository.findAllById(ids)
                .stream()
                .map(Resource::getId)
                .toList();
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
