# Build stage

FROM gradle:jdk21-alpine AS build

WORKDIR /app

COPY gradlew .
COPY gradle gradle/
COPY build.gradle .
COPY settings.gradle .
COPY song-service/build.gradle song-service/

RUN chmod +x gradlew
RUN ./gradlew :song-service:dependencies --no-daemon

COPY song-service/src song-service/src
RUN ./gradlew :song-service:assemble --no-daemon -x test

# Runtime stage

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY --from=build /app/song-service/build/libs/*.jar app.jar

EXPOSE 8082
CMD ["java", "-jar", "app.jar"]