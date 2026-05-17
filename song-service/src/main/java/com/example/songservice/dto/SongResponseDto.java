package com.example.songservice.dto;

public record SongResponseDto(
        Integer id,
        String name,
        String artist,
        String album,
        String duration,
        String year
) {
}
