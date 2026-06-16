package com.example.storageservice.service;

import com.example.storageservice.dto.DeletedIdsDto;
import com.example.storageservice.dto.StorageIdDto;
import com.example.storageservice.dto.StorageRequestDto;
import com.example.storageservice.dto.StorageResponseDto;
import com.example.storageservice.entity.Storage;
import com.example.storageservice.exception.InvalidRequestException;
import com.example.storageservice.repository.StorageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class StorageService {

    private final StorageRepository storageRepository;

    public StorageService(StorageRepository storageRepository) {
        this.storageRepository = storageRepository;
    }

    @Transactional
    public StorageIdDto create(StorageRequestDto dto) {
        Storage storage = new Storage();
        storage.setStorageType(dto.getStorageType());
        storage.setBucket(dto.getBucket());
        storage.setPath(dto.getPath());
        Storage saved = storageRepository.save(storage);
        return new StorageIdDto(saved.getId());
    }

    @Transactional(readOnly = true)
    public List<StorageResponseDto> getAll() {
        return storageRepository.findAll().stream()
                .map(s -> new StorageResponseDto(s.getId(), s.getStorageType(), s.getBucket(), s.getPath()))
                .toList();
    }

    @Transactional
    public DeletedIdsDto delete(String idsCsv) {
        if (idsCsv == null || idsCsv.length() > 200) {
            throw new InvalidRequestException("CSV string must be less than 200 characters");
        }
        List<Integer> ids = parseIds(idsCsv);
        List<Integer> existingIds = storageRepository.findAllById(ids).stream()
                .map(Storage::getId)
                .toList();
        storageRepository.deleteAllById(existingIds);
        return new DeletedIdsDto(existingIds);
    }

    private List<Integer> parseIds(String csv) {
        try {
            return Arrays.stream(csv.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Integer::parseInt)
                    .toList();
        } catch (NumberFormatException e) {
            throw new InvalidRequestException("Invalid ID format in: " + csv);
        }
    }
}
