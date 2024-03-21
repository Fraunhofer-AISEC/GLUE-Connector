#!/bin/bash

set -euo pipefail
prefix="container-pipeline"
artefact="uc-container-orchestration-1.0-SNAPSHOT-all.jar"

cd "$(dirname "$0")"

# Build

docker build -t "$prefix/http-echo" test-containers/http-echo
docker build -t "$prefix/proxy" test-containers/proxy

./gradlew shadowJar

# Run

java -jar "build/libs/$artefact" "$prefix"
