package com.example.songservice.service;

import com.example.songservice.dto.DeletedIdsDto;
import com.example.songservice.dto.SongIdDto;
import com.example.songservice.dto.SongRequestDto;
import com.example.songservice.dto.SongResponseDto;
import com.example.songservice.entity.Song;
import com.example.songservice.exception.InvalidRequestException;
import com.example.songservice.exception.SongAlreadyExistsException;
import com.example.songservice.exception.SongNotFoundException;
import com.example.songservice.repository.SongRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class SongService {

    private final SongRepository songRepository;

    public SongService(SongRepository songRepository) {
        this.songRepository = songRepository;
    }

    @Transactional
    public SongIdDto create(SongRequestDto dto) {
        if (songRepository.existsById(dto.getId())) {
            throw new SongAlreadyExistsException("Metadata for resource ID=" + dto.getId() + " already exists");
        }
        Song song = toEntity(dto);
        songRepository.save(song);
        return new SongIdDto(song.getId());
    }

    @Transactional(readOnly = true)
    public SongResponseDto get(Integer id) {
        if (id <= 0) {
            throw new InvalidRequestException("Invalid value '" + id + "' for ID. Must be a positive integer");
        }
        Song song = songRepository.findById(id)
                .orElseThrow(() -> new SongNotFoundException("Song metadata for ID=" + id + " not found"));
        return toResponseDto(song);
    }

    @Transactional
    public DeletedIdsDto delete(String idsCsv) {
        if (idsCsv.length() > 200) {
            throw new InvalidRequestException("CSV string is too long: received " + idsCsv.length() + " characters, maximum allowed is 200");
        }
        List<Integer> ids = parseIds(idsCsv);
        List<Integer> existingIds = songRepository.findAllById(ids)
                .stream()
                .map(Song::getId)
                .toList();
        songRepository.deleteAllById(existingIds);
        return new DeletedIdsDto(existingIds);
    }

    private Song toEntity(SongRequestDto dto) {
        var song = new Song();
        song.setId(dto.getId());
        song.setName(dto.getName());
        song.setArtist(dto.getArtist());
        song.setAlbum(dto.getAlbum());
        song.setDuration(dto.getDuration());
        song.setYear(dto.getYear());
        return song;
    }

    private SongResponseDto toResponseDto(Song song) {
        return new SongResponseDto(
                song.getId(),
                song.getName(),
                song.getArtist(),
                song.getAlbum(),
                song.getDuration(),
                song.getYear()
        );
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
