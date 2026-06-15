package com.example.resourceservice.controller;

import com.example.resourceservice.messaging.ResourceUploadProducer;
import com.example.resourceservice.client.SongServiceClient;
import com.example.resourceservice.repository.ResourceRepository;
import com.example.resourceservice.service.S3StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class ResourceControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ResourceRepository resourceRepository;

    // Mock external infrastructure so the context loads without real S3/JMS/song-service
    @MockBean
    private S3Client s3Client;
    @MockBean
    private S3StorageService s3StorageService;
    @MockBean
    private ResourceUploadProducer uploadProducer;
    @MockBean
    private SongServiceClient songServiceClient;

    private byte[] mp3Bytes;

    @BeforeEach
    void setUp() throws IOException {
        resourceRepository.deleteAll();
        mp3Bytes = loadMp3();
    }

    @Test
    void uploadResource_validMp3_returns200WithIdAndTriggersS3AndJms() throws Exception {
        MvcResult result = mockMvc.perform(post("/resources")
                        .contentType("audio/mpeg")
                        .content(mp3Bytes))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.length()").value(1))
                .andReturn();

        int id = com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        verify(s3StorageService).upload(anyString(), any(byte[].class));
        verify(uploadProducer).sendResourceId(id);
    }

    @Test
    void uploadResource_nonMp3Bytes_returns400WithExactMessage() throws Exception {
        byte[] json = "{\"key\": \"value\"}".getBytes();

        mockMvc.perform(post("/resources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("400"))
                .andExpect(jsonPath("$.errorMessage")
                        .value("Invalid file format: application/json. Only MP3 files are allowed"))
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    @Test
    void getResource_existingId_returnsMp3WithCorrectContentType() throws Exception {
        when(s3StorageService.download(anyString())).thenReturn(mp3Bytes);
        int id = uploadAndGetId();

        mockMvc.perform(get("/resources/{id}", id))
                .andExpect(status().isOk())
                .andExpect(content().contentType("audio/mpeg"))
                .andExpect(content().bytes(mp3Bytes));
    }

    @Test
    void getResource_afterDelete_returns404WithMessage() throws Exception {
        int id = uploadAndGetId();
        mockMvc.perform(delete("/resources").param("id", String.valueOf(id)));

        mockMvc.perform(get("/resources/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorMessage").value("Resource with ID=" + id + " not found"))
                .andExpect(jsonPath("$.errorCode").value("404"))
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    @Test
    void getResource_nonExistentId_returns404WithMessage() throws Exception {
        mockMvc.perform(get("/resources/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorMessage").value("Resource with ID=99999 not found"))
                .andExpect(jsonPath("$.errorCode").value("404"));
    }

    @Test
    void getResource_letterAsId_returns400() throws Exception {
        mockMvc.perform(get("/resources/ABC"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("400"))
                .andExpect(jsonPath("$.errorMessage").value("Invalid value 'ABC' for ID. Must be a positive integer"));
    }

    @Test
    void getResource_decimalId_returns400() throws Exception {
        mockMvc.perform(get("/resources/1.1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("400"));
    }

    @Test
    void getResource_negativeId_returns400() throws Exception {
        mockMvc.perform(get("/resources/-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("400"))
                .andExpect(jsonPath("$.errorMessage").value("Invalid value '-1' for ID. Must be a positive integer"));
    }

    @Test
    void getResource_zeroId_returns400() throws Exception {
        mockMvc.perform(get("/resources/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("400"))
                .andExpect(jsonPath("$.errorMessage").value("Invalid value '0' for ID. Must be a positive integer"));
    }

    @Test
    void deleteResource_mixedIds_returnsOnlyExistingIdAndCleansUp() throws Exception {
        int id = uploadAndGetId();

        ArgumentCaptor<String> s3KeyCaptor = ArgumentCaptor.forClass(String.class);
        verify(s3StorageService).upload(s3KeyCaptor.capture(), any());

        mockMvc.perform(delete("/resources").param("id", id + ",101,102"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ids").isArray())
                .andExpect(jsonPath("$.ids", hasSize(1)))
                .andExpect(jsonPath("$.ids[0]").value(id));

        verify(s3StorageService).delete(s3KeyCaptor.getValue());
        verify(songServiceClient).deleteSongs(List.of(id));
    }

    @Test
    void deleteResource_nonExistentId_returns200WithEmptyList() throws Exception {
        mockMvc.perform(delete("/resources").param("id", "99999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ids").isArray())
                .andExpect(jsonPath("$.ids", hasSize(0)));
    }

    @Test
    void deleteResource_letterInCsv_returns400() throws Exception {
        mockMvc.perform(delete("/resources").param("id", "1,2,3,4,V"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("400"))
                .andExpect(jsonPath("$.errorMessage").value("Invalid ID format: 'V'. Only positive integers are allowed"));
    }

    @Test
    void deleteResource_csvTooLong_returns400() throws Exception {
        String longCsv = "1,".repeat(100) + "1";

        mockMvc.perform(delete("/resources").param("id", longCsv))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("400"))
                .andExpect(jsonPath("$.errorMessage", containsString("CSV string is too long")));
    }

    private int uploadAndGetId() throws Exception {
        MvcResult result = mockMvc.perform(post("/resources")
                        .contentType("audio/mpeg")
                        .content(mp3Bytes))
                .andExpect(status().isOk())
                .andReturn();
        return com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    private byte[] loadMp3() throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/test-files/valid-sample.mp3")) {
            assertThat(is).as("Test MP3 fixture missing from classpath").isNotNull();
            return is.readAllBytes();
        }
    }
}