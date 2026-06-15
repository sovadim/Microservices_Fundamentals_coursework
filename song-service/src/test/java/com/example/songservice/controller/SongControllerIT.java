package com.example.songservice.controller;

import com.example.songservice.repository.SongRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class SongControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SongRepository songRepository;

    @BeforeEach
    void setUp() {
        songRepository.deleteAll();
    }

    @Test
    void createSong_validPayload_returns200WithIdOnly() throws Exception {
        mockMvc.perform(post("/songs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validSongJson(42)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void createSong_duplicateId_returns409() throws Exception {
        mockMvc.perform(post("/songs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validSongJson(10)));

        mockMvc.perform(post("/songs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validSongJson(10)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorMessage").value("Metadata for resource ID=10 already exists"))
                .andExpect(jsonPath("$.errorCode").value("409"));
    }

    @Test
    void createSong_invalidDuration_returns400WithDurationInDetails() throws Exception {
        mockMvc.perform(post("/songs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(songJson(1, "Test", "Test", "Test", "02:77", "1977")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("400"))
                .andExpect(jsonPath("$.details.duration").exists());
    }

    @Test
    void createSong_invalidYear_returns400WithYearInDetails() throws Exception {
        mockMvc.perform(post("/songs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(songJson(1, "Test", "Test", "Test", "02:59", "01977")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("400"))
                .andExpect(jsonPath("$.details.year").exists());
    }

    @Test
    void createSong_invalidDurationAndYear_returns400WithBothInDetails() throws Exception {
        mockMvc.perform(post("/songs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(songJson(1, "Test", "Test", "Test", "02:77", "01977")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.duration").exists())
                .andExpect(jsonPath("$.details.year").exists());
    }

    @Test
    void createSong_missingName_returns400WithNameMessage() throws Exception {
        mockMvc.perform(post("/songs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id": 1, "artist": "Q", "album": "A", "duration": "02:59", "year": "1977"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.name").value("Song name is required"));
    }

    @Test
    void createSong_allFieldsMissing_returns400WithAllInDetails() throws Exception {
        mockMvc.perform(post("/songs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\": 1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("400"))
                .andExpect(jsonPath("$.details.name").exists())
                .andExpect(jsonPath("$.details.artist").exists())
                .andExpect(jsonPath("$.details.album").exists())
                .andExpect(jsonPath("$.details.duration").exists())
                .andExpect(jsonPath("$.details.year").exists());
    }

    @Test
    void getSong_existingId_returnsExactlySixFields() throws Exception {
        mockMvc.perform(post("/songs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validSongJson(5)));

        mockMvc.perform(get("/songs/5"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.name").value("We are the champions"))
                .andExpect(jsonPath("$.artist").value("Queen"))
                .andExpect(jsonPath("$.album").value("News of the world"))
                .andExpect(jsonPath("$.duration").value("02:59"))
                .andExpect(jsonPath("$.year").value("1977"))
                .andExpect(jsonPath("$.length()").value(6));
    }

    @Test
    void getSong_nonExistentId_returns404WithMessage() throws Exception {
        mockMvc.perform(get("/songs/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorMessage").value("Song metadata for ID=99999 not found"))
                .andExpect(jsonPath("$.errorCode").value("404"))
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    @Test
    void getSong_letterAsId_returns400() throws Exception {
        mockMvc.perform(get("/songs/ABC"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("400"))
                .andExpect(jsonPath("$.errorMessage").value("Invalid value 'ABC' for ID. Must be a positive integer"));
    }

    @Test
    void getSong_decimalId_returns400() throws Exception {
        mockMvc.perform(get("/songs/1.1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("400"));
    }

    @Test
    void getSong_negativeId_returns400() throws Exception {
        mockMvc.perform(get("/songs/-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("400"))
                .andExpect(jsonPath("$.errorMessage").value("Invalid value '-1' for ID. Must be a positive integer"));
    }

    @Test
    void getSong_zeroId_returns400() throws Exception {
        mockMvc.perform(get("/songs/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("400"))
                .andExpect(jsonPath("$.errorMessage").value("Invalid value '0' for ID. Must be a positive integer"));
    }

    @Test
    void deleteSong_mixedIds_returnsOnlyExistingId() throws Exception {
        mockMvc.perform(post("/songs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validSongJson(5)));

        mockMvc.perform(delete("/songs").param("id", "5,101,102"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ids").isArray())
                .andExpect(jsonPath("$.ids", hasSize(1)))
                .andExpect(jsonPath("$.ids[0]").value(5));
    }

    @Test
    void deleteSong_thenGet_returns404() throws Exception {
        mockMvc.perform(post("/songs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validSongJson(7)));

        mockMvc.perform(delete("/songs").param("id", "7"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/songs/7"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorMessage").value("Song metadata for ID=7 not found"))
                .andExpect(jsonPath("$.errorCode").value("404"))
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    @Test
    void deleteSong_nonExistentId_returns200WithEmptyList() throws Exception {
        mockMvc.perform(delete("/songs").param("id", "99999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ids").isArray())
                .andExpect(jsonPath("$.ids", hasSize(0)));
    }

    @Test
    void deleteSong_letterInCsv_returns400() throws Exception {
        mockMvc.perform(delete("/songs").param("id", "1,2,3,4,V"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("400"))
                .andExpect(jsonPath("$.errorMessage").value("Invalid ID format: 'V'. Only positive integers are allowed"));
    }

    @Test
    void deleteSong_csvTooLong_returns400() throws Exception {
        String longCsv = "1,".repeat(100) + "1";

        mockMvc.perform(delete("/songs").param("id", longCsv))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("400"))
                .andExpect(jsonPath("$.errorMessage", containsString("CSV string is too long")));
    }

    private String validSongJson(int id) {
        return songJson(id, "We are the champions", "Queen", "News of the world", "02:59", "1977");
    }

    private String songJson(int id, String name, String artist, String album, String duration, String year) {
        return """
                {"id": %d, "name": "%s", "artist": "%s", "album": "%s", "duration": "%s", "year": "%s"}
                """.formatted(id, name, artist, album, duration, year);
    }
}