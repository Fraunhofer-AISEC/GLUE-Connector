FROM eclipse-temurin:17 AS builder
WORKDIR /src
COPY . .
RUN ./gradlew shadowJar

FROM gcr.io/distroless/java17-debian12
COPY --from=builder /src/build/libs/uc-container-orchestration-1.0-SNAPSHOT-all.jar app.jar
VOLUME /var/run/docker.sock
ENTRYPOINT ["java", "-jar", "app.jar"]
