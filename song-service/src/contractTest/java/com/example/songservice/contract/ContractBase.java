package com.example.songservice.contract;

import com.example.songservice.entity.Song;
import com.example.songservice.repository.SongRepository;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
public class ContractBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SongRepository songRepository;

    @BeforeEach
    public void setUp() {
        RestAssuredMockMvc.mockMvc(mockMvc);
        songRepository.deleteAll();

        Song s5 = new Song();
        s5.setId(5);
        s5.setName("We are the champions");
        s5.setArtist("Queen");
        s5.setAlbum("News of the world");
        s5.setDuration("02:59");
        s5.setYear("1977");
        songRepository.save(s5);

        Song s99 = new Song();
        s99.setId(99);
        s99.setName("Existing");
        s99.setArtist("A");
        s99.setAlbum("B");
        s99.setDuration("02:59");
        s99.setYear("1977");
        songRepository.save(s99);
    }
}