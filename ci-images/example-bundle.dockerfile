# container-registry.gitlab.cc-asp.fraunhofer.de/ccit-glue/usage-control/container-pipeline/ci-example-bundle:latest
FROM debian:trixie-slim
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        ca-certificates \
        curl \
        gettext-base \
        jq \
        jo \
        openssl \
        skopeo \
    && rm -rf /var/lib/apt/lists/*
