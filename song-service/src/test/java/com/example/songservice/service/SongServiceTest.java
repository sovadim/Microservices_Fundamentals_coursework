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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SongServiceTest {

    @Mock
    private SongRepository songRepository;

    @InjectMocks
    private SongService songService;

    @Test
    void create_newSong_returnsSongId() {
        SongRequestDto dto = buildDto(42);
        when(songRepository.existsById(42)).thenReturn(false);
        when(songRepository.save(any(Song.class))).thenAnswer(inv -> inv.getArgument(0));

        SongIdDto result = songService.create(dto);

        assertThat(result.id()).isEqualTo(42);
        verify(songRepository).save(any(Song.class));
    }

    @Test
    void create_duplicateId_throwsSongAlreadyExistsException() {
        SongRequestDto dto = buildDto(42);
        when(songRepository.existsById(42)).thenReturn(true);

        assertThatThrownBy(() -> songService.create(dto))
                .isInstanceOf(SongAlreadyExistsException.class)
                .hasMessage("Metadata for resource ID=42 already exists");
    }

    @Test
    void get_existingId_returnsAllFields() {
        Song song = buildSong(5);
        when(songRepository.findById(5)).thenReturn(Optional.of(song));

        SongResponseDto result = songService.get(5);

        assertThat(result.id()).isEqualTo(5);
        assertThat(result.name()).isEqualTo("We are the champions");
        assertThat(result.artist()).isEqualTo("Queen");
        assertThat(result.album()).isEqualTo("News of the world");
        assertThat(result.duration()).isEqualTo("02:59");
        assertThat(result.year()).isEqualTo("1977");
    }

    @Test
    void get_nonExistentId_throwsSongNotFoundException() {
        when(songRepository.findById(99999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> songService.get(99999))
                .isInstanceOf(SongNotFoundException.class)
                .hasMessage("Song metadata for ID=99999 not found");
    }

    @Test
    void get_zeroId_throwsInvalidRequestException() {
        assertThatThrownBy(() -> songService.get(0))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Invalid value '0' for ID. Must be a positive integer");
    }

    @Test
    void get_negativeId_throwsInvalidRequestException() {
        assertThatThrownBy(() -> songService.get(-1))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Invalid value '-1' for ID. Must be a positive integer");
    }

    @Test
    void delete_existingId_returnsId() {
        Song song = buildSong(5);
        when(songRepository.findAllById(List.of(5))).thenReturn(List.of(song));

        DeletedIdsDto result = songService.delete("5");

        assertThat(result.ids()).containsExactly(5);
        verify(songRepository).deleteAllById(List.of(5));
    }

    @Test
    void delete_nonExistentId_returnsEmptyList() {
        when(songRepository.findAllById(List.of(99999))).thenReturn(List.of());

        DeletedIdsDto result = songService.delete("99999");

        assertThat(result.ids()).isEmpty();
    }

    @Test
    void delete_mixedExistingAndNonExistent_returnsOnlyExisting() {
        Song song = buildSong(5);
        when(songRepository.findAllById(List.of(5, 101, 102))).thenReturn(List.of(song));

        DeletedIdsDto result = songService.delete("5,101,102");

        assertThat(result.ids()).containsExactly(5);
        assertThat(result.ids()).doesNotContain(101, 102);
    }

    @Test
    void delete_csvTooLong_throwsInvalidRequestException() {
        // build a CSV string longer than 200 chars
        String longCsv = "1,".repeat(100) + "1";
        assertThat(longCsv.length()).isGreaterThan(200);

        assertThatThrownBy(() -> songService.delete(longCsv))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("CSV string is too long");
    }

    @Test
    void delete_letterInCsv_throwsInvalidRequestException() {
        assertThatThrownBy(() -> songService.delete("1,2,3,4,V"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Invalid ID format: 'V'. Only positive integers are allowed");
    }

    @Test
    void delete_negativeIdInCsv_throwsInvalidRequestException() {
        assertThatThrownBy(() -> songService.delete("-1"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Invalid ID format: '-1'. Only positive integers are allowed");
    }

    private SongRequestDto buildDto(int id) {
        SongRequestDto dto = new SongRequestDto();
        dto.setId(id);
        dto.setName("We are the champions");
        dto.setArtist("Queen");
        dto.setAlbum("News of the world");
        dto.setDuration("02:59");
        dto.setYear("1977");
        return dto;
    }

    private Song buildSong(int id) {
        Song song = new Song();
        song.setId(id);
        song.setName("We are the champions");
        song.setArtist("Queen");
        song.setAlbum("News of the world");
        song.setDuration("02:59");
        song.setYear("1977");
        return song;
    }
}