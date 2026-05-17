package com.example.resourceservice.controller;

import com.example.resourceservice.dto.DeletedIdsDto;
import com.example.resourceservice.dto.ResourceIdDto;
import com.example.resourceservice.service.ResourceService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/resources")
public class ResourceController {

    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @PostMapping
    public ResponseEntity<ResourceIdDto> upload(@RequestBody byte[] data) {
        return ResponseEntity.ok(resourceService.upload(data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> get(@PathVariable Integer id) {
        byte[] data = resourceService.get(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .body(data);
    }

    @DeleteMapping
    public ResponseEntity<DeletedIdsDto> delete(@RequestParam String id) {
        return ResponseEntity.ok(resourceService.delete(id));
    }
}
