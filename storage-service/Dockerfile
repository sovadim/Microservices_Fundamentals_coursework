FROM gradle:jdk21-alpine AS build

WORKDIR /app

COPY gradlew .
COPY gradle gradle/
COPY build.gradle .
COPY settings.gradle .
COPY storage-service/build.gradle storage-service/

RUN chmod +x gradlew
RUN ./gradlew :storage-service:dependencies --no-daemon

COPY storage-service/src storage-service/src
RUN ./gradlew :storage-service:assemble --no-daemon -x test

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY --from=build /app/storage-service/build/libs/*.jar app.jar

EXPOSE 8084
CMD ["java", "-jar", "app.jar"]
