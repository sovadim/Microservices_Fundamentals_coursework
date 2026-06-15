package com.example.songservice.component;

import com.example.songservice.repository.SongRepository;
import com.jayway.jsonpath.JsonPath;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public class SongSteps {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SongRepository songRepository;

    private MvcResult lastResult;

    @Before
    public void cleanDatabase() {
        songRepository.deleteAll();
    }

    @Given("the song repository is empty")
    public void theSongRepositoryIsEmpty() {
        songRepository.deleteAll();
    }

    @When("I create a song with id {int}, name {string}, artist {string}, album {string}, duration {string}, year {string}")
    public void iCreateASong(int id, String name, String artist, String album, String duration, String year) throws Exception {
        String body = """
                {"id":%d,"name":"%s","artist":"%s","album":"%s","duration":"%s","year":"%s"}
                """.formatted(id, name, artist, album, duration, year);
        lastResult = mockMvc.perform(post("/songs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();
    }

    @When("I retrieve song with id {int}")
    public void iRetrieveSong(int id) throws Exception {
        lastResult = mockMvc.perform(get("/songs/{id}", id)).andReturn();
    }

    @When("I delete songs with ids {string}")
    public void iDeleteSongs(String ids) throws Exception {
        lastResult = mockMvc.perform(delete("/songs").param("id", ids)).andReturn();
    }

    @Then("the response status is {int}")
    public void theResponseStatusIs(int status) {
        assertThat(lastResult.getResponse().getStatus()).isEqualTo(status);
    }

    @Then("the response contains id {int}")
    public void theResponseContainsId(int id) throws Exception {
        int actual = JsonPath.read(lastResult.getResponse().getContentAsString(), "$.id");
        assertThat(actual).isEqualTo(id);
    }

    @Then("the response contains name {string}")
    public void theResponseContainsName(String name) throws Exception {
        String actual = JsonPath.read(lastResult.getResponse().getContentAsString(), "$.name");
        assertThat(actual).isEqualTo(name);
    }

    @Then("the response has a validation error on field {string}")
    public void theResponseHasValidationError(String field) throws Exception {
        Object details = JsonPath.read(lastResult.getResponse().getContentAsString(), "$.details." + field);
        assertThat(details).isNotNull();
    }

    @Then("the error message is {string}")
    public void theErrorMessageIs(String message) throws Exception {
        String actual = JsonPath.read(lastResult.getResponse().getContentAsString(), "$.errorMessage");
        assertThat(actual).isEqualTo(message);
    }

    @Then("the deleted ids list contains {int}")
    public void theDeletedIdsListContains(int id) throws Exception {
        List<Integer> ids = JsonPath.read(lastResult.getResponse().getContentAsString(), "$.ids");
        assertThat(ids).contains(id);
    }
}