package com.example.resourceservice.component;

import com.example.resourceservice.repository.ResourceRepository;
import com.example.resourceservice.service.S3StorageService;
import com.jayway.jsonpath.JsonPath;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public class ResourceSteps {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private S3StorageService s3StorageService;

    private MvcResult lastResult;
    private int lastUploadedId;
    private byte[] mp3Bytes;

    @Before
    public void setUp() throws IOException {
        resourceRepository.deleteAll();
        mp3Bytes = loadMp3();
    }

    @Given("the resource repository is empty")
    public void theResourceRepositoryIsEmpty() {
        resourceRepository.deleteAll();
    }

    @When("I upload a valid MP3 file")
    public void iUploadValidMp3() throws Exception {
        lastResult = mockMvc.perform(post("/resources")
                        .contentType("audio/mpeg")
                        .content(mp3Bytes))
                .andReturn();
        if (lastResult.getResponse().getStatus() == 200) {
            lastUploadedId = JsonPath.read(lastResult.getResponse().getContentAsString(), "$.id");
        }
    }

    @When("I upload JSON bytes as {string}")
    public void iUploadJsonBytes(String contentType) throws Exception {
        lastResult = mockMvc.perform(post("/resources")
                        .contentType(contentType)
                        .content("{\"key\":\"value\"}".getBytes()))
                .andReturn();
    }

    @When("I retrieve the last uploaded resource")
    public void iRetrieveLastUploadedResource() throws Exception {
        when(s3StorageService.download(anyString())).thenReturn(mp3Bytes);
        lastResult = mockMvc.perform(get("/resources/{id}", lastUploadedId)).andReturn();
    }

    @When("I delete the uploaded resource along with non-existent ids {string}")
    public void iDeleteUploadedResourceAlongWith(String extraIds) throws Exception {
        String ids = lastUploadedId + "," + extraIds;
        lastResult = mockMvc.perform(delete("/resources").param("id", ids)).andReturn();
    }

    @Then("the response status is {int}")
    public void theResponseStatusIs(int status) {
        assertThat(lastResult.getResponse().getStatus()).isEqualTo(status);
    }

    @Then("the response body contains a numeric id")
    public void theResponseBodyContainsNumericId() throws Exception {
        int id = JsonPath.read(lastResult.getResponse().getContentAsString(), "$.id");
        assertThat(id).isPositive();
    }

    @Then("the response content type is {string}")
    public void theResponseContentTypeIs(String contentType) {
        assertThat(lastResult.getResponse().getContentType()).startsWith(contentType);
    }

    @Then("the response errorCode is {string}")
    public void theResponseErrorCodeIs(String errorCode) throws Exception {
        String actual = JsonPath.read(lastResult.getResponse().getContentAsString(), "$.errorCode");
        assertThat(actual).isEqualTo(errorCode);
    }

    @Then("the response errorMessage contains {string}")
    public void theResponseErrorMessageContains(String fragment) throws Exception {
        String actual = JsonPath.read(lastResult.getResponse().getContentAsString(), "$.errorMessage");
        assertThat(actual).contains(fragment);
    }

    @Then("the deleted ids list has size {int}")
    public void theDeletedIdsListHasSize(int size) throws Exception {
        List<Integer> ids = JsonPath.read(lastResult.getResponse().getContentAsString(), "$.ids");
        assertThat(ids).hasSize(size);
    }

    private byte[] loadMp3() throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/test-files/valid-sample.mp3")) {
            assertThat(is).as("Test MP3 fixture missing from classpath").isNotNull();
            return is.readAllBytes();
        }
    }
}