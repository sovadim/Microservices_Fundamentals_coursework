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

* Eureka: http://localhost:8761
* Resource service: http://localhost:8081
* Song service: http://localhost:8082
