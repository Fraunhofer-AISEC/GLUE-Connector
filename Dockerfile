FROM eclipse-temurin:21 AS builder
WORKDIR /src
COPY . .
RUN --mount=type=cache,target=/root/.gradle ./gradlew --no-daemon shadowJar
RUN sleep 10s

FROM gcr.io/distroless/java21-debian12
# TODO: Also create a version-independent shadow jar
COPY --from=builder /src/build/libs/uc-container-orchestration-1.0-SNAPSHOT-all.jar app.jar
VOLUME /var/run/docker.sock
ENTRYPOINT ["java", "-jar", "app.jar"]
