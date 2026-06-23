package com.example.e2e.steps;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class FullFlowSteps {

    private static final String TOKEN = fetchAdminToken();

    private static final RequestSpecification SPEC = new RequestSpecBuilder()
            .setBaseUri(System.getProperty("e2e.base.url", "http://localhost:8080"))
            .addHeader("Authorization", "Bearer " + TOKEN)
            .build();

    private static String fetchAdminToken() {
        String authUrl = System.getProperty("e2e.auth.url", "http://localhost:9000");
        return given()
                .baseUri(authUrl)
                .auth().preemptive().basic("admin-client", "admin-secret")
                .contentType("application/x-www-form-urlencoded")
                .formParam("grant_type", "client_credentials")
                .formParam("scope", "ROLE_ADMIN")
                .post("/oauth2/token")
                .jsonPath()
                .getString("access_token");
    }

    private byte[] mp3Bytes;
    private int resourceId;
    private Response lastResponse;

    @Given("a valid MP3 file is ready")
    public void aValidMp3FileIsReady() throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/test-files/valid-sample-with-required-tags.mp3")) {
            assertThat(is).as("MP3 fixture missing from classpath").isNotNull();
            mp3Bytes = is.readAllBytes();
        }
    }

    @When("I upload the MP3 via POST \\/resources")
    public void uploadMp3() {
        lastResponse = given(SPEC)
                .contentType("audio/mpeg")
                .body(mp3Bytes)
                .when().post("/resources");
    }

    @When("I POST random bytes with content-type {string}")
    public void postRandomBytesWithContentType(String contentType) {
        lastResponse = given(SPEC)
                .contentType(contentType)
                .body(new byte[]{0x00, 0x01, 0x02, 0x03})
                .when().post("/resources");
    }

    @When("I GET \\/resources\\/{int}")
    public void getResourceById(int id) {
        lastResponse = given(SPEC).when().get("/resources/" + id);
    }

    @When("I delete the uploaded resource")
    public void deleteUploadedResource() {
        lastResponse = given(SPEC).when().delete("/resources?id=" + resourceId);
    }

    @Then("the response status is {int}")
    public void responseStatusIs(int expectedStatus) {
        assertThat(lastResponse.statusCode()).isEqualTo(expectedStatus);
    }

    @Then("a resource id is returned")
    public void resourceIdIsReturned() {
        resourceId = lastResponse.jsonPath().getInt("id");
        assertThat(resourceId).isPositive();
    }

    @Then("within {int} seconds the song metadata is available")
    public void songMetadataIsAvailable(int timeoutSeconds) {
        await()
                .atMost(timeoutSeconds, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    Response r = given(SPEC).get("/songs/" + resourceId);
                    assertThat(r.statusCode()).isEqualTo(200);
                });
    }

    @Then("the song has name {string}, artist {string}, album {string}, year {string}")
    public void songHasMetadata(String name, String artist, String album, String year) {
        Response r = given(SPEC).get("/songs/" + resourceId);
        assertThat(r.jsonPath().getString("name")).isEqualTo(name);
        assertThat(r.jsonPath().getString("artist")).isEqualTo(artist);
        assertThat(r.jsonPath().getString("album")).isEqualTo(album);
        assertThat(r.jsonPath().getString("year")).isEqualTo(year);
        assertThat(r.jsonPath().getString("duration")).matches("\\d{2}:\\d{2}");
    }

    @Then("the uploaded resource returns {int}")
    public void uploadedResourceReturns(int expectedStatus) {
        Response r = given(SPEC).get("/resources/" + resourceId);
        assertThat(r.statusCode()).isEqualTo(expectedStatus);
    }

    @Then("the song metadata returns {int}")
    public void songMetadataReturns(int expectedStatus) {
        Response r = given(SPEC).get("/songs/" + resourceId);
        assertThat(r.statusCode()).isEqualTo(expectedStatus);
    }

    @Then("the error code is {string}")
    public void errorCodeIs(String expectedCode) {
        assertThat(lastResponse.jsonPath().getString("errorCode")).isEqualTo(expectedCode);
    }
}