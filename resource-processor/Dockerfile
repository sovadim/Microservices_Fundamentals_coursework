# Build stage

FROM gradle:jdk21-alpine AS build

WORKDIR /app

COPY gradlew .
COPY gradle gradle/
COPY build.gradle .
COPY settings.gradle .
COPY resource-processor/build.gradle resource-processor/

RUN chmod +x gradlew
RUN ./gradlew :resource-processor:dependencies --no-daemon

COPY resource-processor/src resource-processor/src
RUN ./gradlew :resource-processor:assemble --no-daemon -x test

# Runtime stage

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY --from=build /app/resource-processor/build/libs/*.jar app.jar

EXPOSE 8083
CMD ["java", "-jar", "app.jar"]
