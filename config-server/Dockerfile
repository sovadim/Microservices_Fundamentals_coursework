# Build stage

FROM gradle:jdk21-alpine AS build

WORKDIR /app

COPY gradlew .
COPY gradle gradle/
COPY build.gradle .
COPY settings.gradle .
COPY config-server/build.gradle config-server/

RUN chmod +x gradlew
RUN ./gradlew :config-server:dependencies --no-daemon

COPY config-server/src config-server/src
RUN ./gradlew :config-server:assemble --no-daemon -x test

# Runtime stage

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY --from=build /app/config-server/build/libs/*.jar app.jar

EXPOSE 8888
CMD ["java", "-jar", "app.jar"]
