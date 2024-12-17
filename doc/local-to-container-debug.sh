#!/usr/bin/env bash

set -e

./gradlew jibDockerBuild
docker stop connector || true
docker run --rm --name connector -v /var/run/docker.sock:/var/run/docker.sock -d uc-container-orchestration:1.0-SNAPSHOT
host_connector_ip="$(docker inspect -f '{{ .NetworkSettings.Networks.bridge.Gateway }}' "connector")"
docker_connector_ip="$(docker inspect -f '{{ .NetworkSettings.Networks.bridge.IPAddress }}' "connector")"
while ! nc -z "$docker_connector_ip" 8080; do
  sleep 1
done
./doc/api-example.sh "$docker_connector_ip:8080" "$host_connector_ip:8080" test-containers/example-policy.json
