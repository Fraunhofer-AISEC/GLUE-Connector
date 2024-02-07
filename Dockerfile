FROM eclipse-temurin:17 AS builder
WORKDIR /src
COPY . .
# TODO: Cache the gradle wrapper?
RUN ./gradlew shadowJar

# TODO: Switch to java21-debian12 when the image becomes available
FROM gcr.io/distroless/java17-debian12
# TODO: Also create a version-independent shadow jar
COPY --from=builder /src/build/libs/uc-container-orchestration-1.0-SNAPSHOT-all.jar app.jar
VOLUME /var/run/docker.sock
ENTRYPOINT ["java", "-jar", "app.jar"]
