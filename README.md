# Microservices Fundamentals coursework

## Running locally

### With Docker Compose

Run:
```bash
docker compose up -d --build
```

To scale the services, run:
```bash
docker compose up -d --build --scale service_name=<number_of_instances>
```

To check logs:
```bash
docker compose logs -f resource-service song-service
```

Stop:
```bash
docker compose down
```

### With gradle

Run databases in docker:
```bash
docker compose up -d resource-db song-db
```

Run services
```bash
./gradlew :eureka-server:bootRun
./gradlew :song-service:bootRun
./gradlew :resource-service:bootRun
```

### Endpoints

* Resource service: http://localhost:8081
* Song service: http://localhost:8082
* Eureka: http://localhost:8761
* API Gateway: http://localhost:8080

* S3 Web UI: http://localhost:9001
* Active MQ Web UI: http://localhost:8161

## API Reference

### Resource Service (`/resources`)

#### Upload resource

```
POST /resources
Content-Type: audio/mpeg
Body: <binary MP3 data>
```

Stores an MP3 file, extracts its metadata, and forwards the metadata to Song Service.

Response `200 OK`:
```json
{ "id": 1 }
```

#### Get resource

```
GET /resources/{id}
```

Returns the raw MP3 audio bytes for the given resource ID.

Response `200 OK`: binary `audio/mpeg` body.

| Status | Condition |
|--------|-----------|
| 400 | ID is invalid |
| 404 | Resource not found |

#### Delete resources

```
DELETE /resources?id=1,2,3
```

Deletes resources by comma-separated IDs (max 200 characters). Missing IDs are silently ignored.

Response `200 OK`:
```json
{ "ids": [1, 2, 3] }
```

| Status | Condition |
|--------|-----------|
| 400 | CSV string is malformed or exceeds 200 characters |

### Song Service (`/songs`)

#### Create song metadata

```
POST /songs
Content-Type: application/json
```

Request body:
```json
{
    "id": 1,
    "name": "We are the champions",
    "artist": "Queen",
    "album": "News of the world",
    "duration": "02:59",
    "year": "1977"
}
```

| Field | Rules |
|-------|-------|
| `id` | Positive integer, must match an existing Resource ID |
| `name` | 1–100 characters |
| `artist` | 1–100 characters |
| `album` | 1–100 characters |
| `duration` | `mm:ss` format with leading zeros |
| `year` | `YYYY`, 1900–2099 |

Response `200 OK`:
```json
{ "id": 1 }
```

| Status | Condition |
|--------|-----------|
| 400 | Validation failed |
| 409 | Metadata for this ID already exists |

#### Get song metadata

```
GET /songs/{id}
```

Response `200 OK`:
```json
{
    "id": 1,
    "name": "We are the champions",
    "artist": "Queen",
    "album": "News of the world",
    "duration": "02:59",
    "year": "1977"
}
```

| Status | Condition |
|--------|-----------|
| 400 | ID is invalid |
| 404 | Song metadata not found |

#### Delete songs metadata

```
DELETE /songs?id=1,2,3
```

Deletes song metadata records by comma-separated IDs (max 200 characters). Missing IDs are silently ignored.

Response `200 OK`:
```json
{ "ids": [1, 2, 3] }
```

| Status | Condition |
|--------|-----------|
| 400 | CSV string is malformed or exceeds 200 characters |

### Error response format

```json
{ "errorMessage": "Resource with ID=1 not found", "errorCode": "404" }
```

Validation errors include a `details` map:
```json
{
    "errorMessage": "Validation error",
    "details": {
        "duration": "Duration must be in mm:ss format with leading zeros",
        "year": "Year must be between 1900 and 2099"
    },
    "errorCode": "400"
}
```
