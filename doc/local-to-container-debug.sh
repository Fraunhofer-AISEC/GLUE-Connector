#!/usr/bin/env bash

docker_path="/var/run/docker.sock"
#docker_path=user/1000/podman/podman.sock"
export DOCKER_HOST="unix://$docker_path"

set -e

./gradlew jibDockerBuild
docker stop connector || true
sleep 1
docker run --rm --name connector -e AUTH_USERNAME=user -e AUTH_PASSWORD=password -v "$docker_path":/var/run/docker.sock -d uc-container-orchestration:1.0-SNAPSHOT
sleep 1
host_connector_ip="$(docker inspect -f '{{ .NetworkSettings.Networks.bridge.Gateway }}' "connector")"
docker_connector_ip="$(docker inspect -f '{{ .NetworkSettings.Networks.bridge.IPAddress }}' "connector")"
while ! nc -z "$docker_connector_ip" 8080; do
  sleep 1
done
./doc/api-example.sh "$docker_connector_ip:8080" "$host_connector_ip:8080" test-containers/example-policy.json

