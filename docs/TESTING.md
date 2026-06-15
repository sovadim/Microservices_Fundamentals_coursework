# Testing Strategy

## Overview

The system consists of three microservices — **resource-service**, **song-service**, and **resource-processor** - communicating over HTTP (via api-gateway/Eureka) and messaging (ActiveMQ). This document describes the testing approach to ensure correctness at each layer.

## Test Types and Responsibilities

| Type | Tool | What it covers |
|---|---|---|
| Unit | JUnit 5 + Mockito | Business logic and validation rules isolated from infrastructure |
| Integration | Spring Boot Test + MockMvc + H2 | Full HTTP → Service → DB request cycle within one service |
| Component | Cucumber + Spring Boot Test | Business scenarios in natural language, service-level |
| Contract | Spring Cloud Contract | HTTP and messaging contracts between services |
| E2E | Cucumber + RestAssured | Full user journeys across the live stack |

## Test Distribution

```
resource-service
├── unit        ResourceServiceTest, Mp3MetadataExtractorTest
├── integration ResourceControllerIT
└── component   resource_management.feature

song-service
├── unit        SongServiceTest
├── integration SongControllerIT
└── component   song_management.feature

resource-processor
└── unit        ResourceUploadConsumerTest

contracts/
├── HTTP        song-service produces stubs consumed by resource-service client tests
└── Messaging   resource-service produces, resource-processor consumes

e2e-tests/
└── full_flow.feature (runs against live Docker Compose stack)
```

## 1. Unit Tests

Target: pure business logic with all external dependencies mocked via Mockito.

**song-service** — `SongServiceTest`
- Create: happy path returns id; duplicate id throws 409 exception
- Get: found returns dto; not found throws 404; zero/negative id throws 400
- Delete: returns only existing ids; CSV too long throws 400; invalid chars throw 400

**resource-service** — `ResourceServiceTest`, `Mp3MetadataExtractorTest`
- Upload: valid MP3 saves to DB+S3 and publishes JMS message; non-MP3 or empty throws 400
- Get: found returns S3 bytes; not found throws 404; invalid id throws 400
- Delete: deletes from S3, DB, and propagates to song-service; only existing ids returned
- `isValidMp3`: real MP3 returns true; plain bytes return false
- `extract`: valid MP3 with ID3 tags returns populated dto; duration formatted as mm:ss

**resource-processor** — `ResourceUploadConsumerTest`
- On message: fetches resource, extracts metadata, posts to song-service with correct id
- On fetch failure: exception propagates

## 2. Integration Tests

Target: verify the full HTTP stack within a service using an in-memory H2 database. External dependencies (S3, JMS, other services) are mocked via `@MockBean`.

**song-service** — `SongControllerIT`
- POST /songs: creates record → GET /songs/{id} returns it → DELETE removes it → GET returns 404
- POST /songs with invalid duration/year: 400 with `details` map
- POST /songs with missing fields: 400 with `details` map
- POST /songs with duplicate id: 409

**resource-service** — `ResourceControllerIT`
- POST /resources with valid MP3: 200 with id
- GET /resources/{id}: 200 with audio/mpeg content
- DELETE /resources?id=X,101,102: returns only existing id
- POST /resources with non-MP3 bytes: 400 with exact error message
- GET /resources/ABC, /resources/-1, /resources/0: 400 with correct messages

## 3. Component Tests

Target: business scenarios described in natural language, running against the full Spring context with mocked infra. Placed alongside integration tests in each service.

**song-service** — `song_management.feature`
```gherkin
Scenario: Create and retrieve song metadata
Scenario: Reject invalid duration format
Scenario: Reject duplicate song id
Scenario: Delete song and confirm removal
```

**resource-service** — `resource_management.feature`
```gherkin
Scenario: Upload valid MP3 and retrieve binary content
Scenario: Reject non-MP3 upload with 400 error
Scenario: Delete resource returns only existing ids
```

## 4. Contract Tests (Spring Cloud Contract)

Target: ensure service boundaries remain stable as services evolve independently.

**HTTP contracts** — song-service is the producer, resource-service is the consumer:
- `POST /songs` → 200 `{id}`
- `POST /songs` with duplicate id → 409 `{errorMessage, errorCode}`
- `DELETE /songs?id=CSV` → 200 `{ids}`
- `GET /songs/{id}` → 200 with all 6 fields

song-service verifier tests run against the real Spring context and generate WireMock stubs. resource-service uses `@AutoConfigureStubRunner` to verify its `SongServiceClient` calls match the stub.

**Messaging contracts** — resource-service is the producer, resource-processor is the consumer:
- `resource-upload` queue message: `"{resourceId}"` (string)

resource-service contract verifier test triggers `uploadProducer.sendResourceId(1)` and asserts the message shape. resource-processor stub runner verifies the consumer handles the same message shape.

## 5. End-to-End Tests

Target: full user journeys via the api-gateway (`http://localhost:8080`). These require the complete Docker Compose stack to be running.

**`full_flow.feature`**
```gherkin
Scenario: Upload MP3, wait for metadata, then delete everything
Scenario: Invalid content type is rejected at gateway
Scenario: Non-existent resource returns 404
```

Step definitions use RestAssured. Tests are tagged `@E2E` and excluded from the normal `./gradlew test` task. Run with: `./gradlew e2eTest` (requires running stack).

## Running Tests

```bash
# Unit + integration + component + contract (no infra needed)
./gradlew test

# E2E (requires: docker compose up)
./gradlew e2eTest
```