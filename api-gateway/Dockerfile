# Build stage

FROM gradle:jdk21-alpine AS build

WORKDIR /app

COPY gradlew .
COPY gradle gradle/
COPY build.gradle .
COPY settings.gradle .
COPY api-gateway/build.gradle api-gateway/

RUN chmod +x gradlew
RUN ./gradlew :api-gateway:dependencies --no-daemon

COPY api-gateway/src api-gateway/src
RUN ./gradlew :api-gateway:assemble --no-daemon -x test

# Runtime stage

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY --from=build /app/api-gateway/build/libs/*.jar app.jar

EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
