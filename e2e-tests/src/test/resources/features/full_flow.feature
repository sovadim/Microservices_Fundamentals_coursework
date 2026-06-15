@E2E
Feature: Full flow across the live Docker Compose stack

  Scenario: Upload MP3, wait for async metadata processing, then delete everything
    Given a valid MP3 file is ready
    When I upload the MP3 via POST /resources
    Then the response status is 200
    And a resource id is returned
    And within 15 seconds the song metadata is available
    And the song has name "Test Title", artist "Test Artist", album "Test Album", year "2025"
    When I delete the uploaded resource
    Then the response status is 200
    And the uploaded resource returns 404
    And the song metadata returns 404

  Scenario: Invalid content type is rejected at the gateway
    When I POST random bytes with content-type "audio/mpeg"
    Then the response status is 400
    And the error code is "400"

  Scenario: Non-existent resource returns 404
    When I GET /resources/99999
    Then the response status is 404
    And the error code is "404"
