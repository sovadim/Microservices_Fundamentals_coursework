# Build stage

FROM gradle:jdk21-alpine AS build

WORKDIR /app

COPY gradlew .
COPY gradle gradle/
COPY build.gradle .
COPY settings.gradle .
COPY eureka-server/build.gradle eureka-server/

RUN chmod +x gradlew
RUN ./gradlew :eureka-server:dependencies --no-daemon

COPY eureka-server/src eureka-server/src
RUN ./gradlew :eureka-server:assemble --no-daemon -x test

# Runtime stage

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY --from=build /app/eureka-server/build/libs/*.jar app.jar

EXPOSE 8761
CMD ["java", "-jar", "app.jar"]