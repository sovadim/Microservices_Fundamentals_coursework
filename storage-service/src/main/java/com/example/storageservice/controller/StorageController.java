package com.example.storageservice.controller;

import com.example.storageservice.dto.DeletedIdsDto;
import com.example.storageservice.dto.StorageIdDto;
import com.example.storageservice.dto.StorageRequestDto;
import com.example.storageservice.dto.StorageResponseDto;
import com.example.storageservice.service.StorageService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/storages")
public class StorageController {

    private final StorageService storageService;

    public StorageController(StorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping
    public ResponseEntity<StorageIdDto> create(@RequestBody @Valid StorageRequestDto dto) {
        return ResponseEntity.ok(storageService.create(dto));
    }

    @GetMapping
    public ResponseEntity<List<StorageResponseDto>> getAll() {
        return ResponseEntity.ok(storageService.getAll());
    }

    @DeleteMapping
    public ResponseEntity<DeletedIdsDto> delete(@RequestParam String id) {
        return ResponseEntity.ok(storageService.delete(id));
    }
}
