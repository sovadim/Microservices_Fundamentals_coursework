Feature: Song metadata management

  Background:
    Given the song repository is empty

  Scenario: Create and retrieve song metadata
    When I create a song with id 42, name "We are the champions", artist "Queen", album "News of the world", duration "02:59", year "1977"
    Then the response status is 200
    And the response contains id 42
    When I retrieve song with id 42
    Then the response status is 200
    And the response contains name "We are the champions"

  Scenario: Reject song with invalid duration
    When I create a song with id 1, name "Test", artist "Test", album "Test", duration "02:77", year "1977"
    Then the response status is 400
    And the response has a validation error on field "duration"

  Scenario: Reject duplicate song id
    When I create a song with id 10, name "Original", artist "A", album "B", duration "02:59", year "1977"
    And I create a song with id 10, name "Duplicate", artist "A", album "B", duration "02:59", year "1977"
    Then the response status is 409
    And the error message is "Metadata for resource ID=10 already exists"

  Scenario: Delete song and confirm removal
    When I create a song with id 7, name "Test", artist "A", album "B", duration "02:59", year "1977"
    And I delete songs with ids "7"
    Then the response status is 200
    And the deleted ids list contains 7
    When I retrieve song with id 7
    Then the response status is 404