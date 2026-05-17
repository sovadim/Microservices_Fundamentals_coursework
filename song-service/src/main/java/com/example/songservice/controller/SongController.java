package com.example.songservice.controller;

import com.example.songservice.dto.DeletedIdsDto;
import com.example.songservice.dto.SongIdDto;
import com.example.songservice.dto.SongRequestDto;
import com.example.songservice.dto.SongResponseDto;
import com.example.songservice.service.SongService;
import jakarta.validation.Valid;
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
@RequestMapping("/songs")
public class SongController {

    private final SongService songService;

    public SongController(SongService songService) {
        this.songService = songService;
    }

    @PostMapping
    public ResponseEntity<SongIdDto> create(@RequestBody @Valid SongRequestDto dto) {
        return ResponseEntity.ok(songService.create(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SongResponseDto> get(@PathVariable Integer id) {
        return ResponseEntity.ok(songService.get(id));
    }

    @DeleteMapping
    public ResponseEntity<DeletedIdsDto> delete(@RequestParam String id) {
        return ResponseEntity.ok(songService.delete(id));
    }
}