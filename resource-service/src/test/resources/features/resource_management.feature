Feature: Resource management

  Background:
    Given the resource repository is empty

  Scenario: Upload valid MP3 and retrieve binary content
    When I upload a valid MP3 file
    Then the response status is 200
    And the response body contains a numeric id
    When I retrieve the last uploaded resource
    Then the response status is 200
    And the response content type is "audio/mpeg"

  Scenario: Reject non-MP3 upload
    When I upload JSON bytes as "application/json"
    Then the response status is 400
    And the response errorCode is "400"
    And the response errorMessage contains "Only MP3 files are allowed"

  Scenario: Delete resource returns only existing ids
    When I upload a valid MP3 file
    And I delete the uploaded resource along with non-existent ids "101,102"
    Then the response status is 200
    And the deleted ids list has size 1