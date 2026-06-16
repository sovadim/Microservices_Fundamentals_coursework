FROM gradle:jdk21-alpine AS build

WORKDIR /app

COPY gradlew .
COPY gradle gradle/
COPY build.gradle .
COPY settings.gradle .
COPY authorization-server/build.gradle authorization-server/

RUN chmod +x gradlew
RUN ./gradlew :authorization-server:dependencies --no-daemon

COPY authorization-server/src authorization-server/src
RUN ./gradlew :authorization-server:assemble --no-daemon -x test

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY --from=build /app/authorization-server/build/libs/*.jar app.jar

EXPOSE 9000
CMD ["java", "-jar", "app.jar"]
