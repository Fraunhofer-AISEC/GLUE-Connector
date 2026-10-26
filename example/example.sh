#!/bin/bash

set -euo pipefail
cd "$(dirname "$0")"

BOLD='\033[1m'
RED='\033[31m'
BLUE='\033[34m'
RESET='\033[0m'

require() {
  missing_progs=()
  for prog in "$@"; do
    if ! command -v "$prog" > /dev/null; then
      missing_progs+=("$prog")
    fi
  done

  if (( ${#missing_progs[@]} != 0 )); then
    echo "The following programs are required but not in path: ${missing_progs[*]}" >&2
    return 1
  fi
}

info() {
  printf "${BOLD}${BLUE}==> ${RESET}${BOLD}%s${RESET}\n" "$*" >&2
}

detail() {
  printf "    ${BOLD}%s${RESET}\n" "$*" >&2
}

error() {
  printf "${BOLD}${RED}==> ${RESET}${BOLD}%s${RESET}\n" "$*" >&2
}

print_help() {
  printf "Usage: %s [Options] <example name>\n" "$0"
  printf "Options:\n"
  printf "  --insecure        Disable aTLS support\n"
  printf "  --regenerate-pki  Generate new certificates\n"
  printf "  --refetch-images  Re-fetch all container images from the registry\n"
  printf "  --help            Show this message and exit\n"
}

# Simple Go-style exit handler stack

_defer_handlers=()

defer() {
  _defer_handlers+=("$*")
}

_run_deferred() {
  set +e
  for (( i=${#_defer_handlers[@]}-1; i>=0; i-- )); do
      eval "${_defer_handlers[$i]}"
  done
  set -e
}
trap _run_deferred EXIT

insecure=false
regenerate_pki=false
refetch_images=false

while [[ $# -gt 0 ]]; do
  case $1 in
    --insecure)
      insecure=true
      shift
      ;;
    --regenerate-pki)
      regenerate_pki=true
      shift
      ;;
    --refetch-images)
      refetch_images=true
      shift
      ;;
    --help)
      print_help >&2
      exit 0
      ;;
    *)
      break
      ;;
  esac
done

if [[ $# -eq 0 ]]; then
  print_help >&2
  exit 1
fi

require curl docker envsubst head dirname yq jo sed skopeo sleep tr

if [ ! -d pki ] || ${regenerate_pki}; then
    require mkdir openssl rm
fi

if [ ! -d images ] || ${refetch_images}; then
  require mkdir
fi

# shellcheck disable=SC2015
compose_file="$1$($insecure && echo "-insecure" || true).compose.yml"
policy_file="$1.policy.json"
compose_project_name="example_$(echo "$1" | tr "-" "_")"

if [ ! -f "${compose_file}" ] || [ ! -f "${policy_file}" ]; then
  echo "Example $1 does not exist" >&2
  exit 1
fi

if [ ! -d pki ] || ${regenerate_pki}; then
    info "Generating new PKI certificates"
    ./scripts/generate-pki.sh
fi

if [ ! -d images ] || ${refetch_images}; then
    info "Preparing example image archives"
    ./scripts/prepare-images.sh
fi

connector_image="container-pipeline:latest"
info "Loading connector image into local docker daemon"
skopeo copy --insecure-policy --override-os linux "oci-archive:images/container-pipeline.tar" "docker-daemon:${connector_image}"

info "Starting docker compose environment"

docker compose -p "${compose_project_name}" -f "${compose_file}" up -d --build

remove_compose() {
  info "Removing docker compose environment"
  docker compose -p "${compose_project_name}" -f "${compose_file}" down -v
}
defer remove_compose

initial_host="$(yq -r '.["x-example-metadata"]["initial-host"]' < "$compose_file")"
intermediary_host="$(yq -r '.["x-example-metadata"]["intermediary-host"]' < "$compose_file")"
start_script=$(yq -r '.["x-example-metadata"]["start-script"] // "start-pipeline.sh"' < "$compose_file")

initial_container_ip="$(docker inspect -f "{{ .NetworkSettings.Networks.${compose_project_name}_default.IPAddress }}" "${compose_project_name}-${initial_host}-connector-1")"
intermediary_container_ip="$(docker inspect -f "{{ .NetworkSettings.Networks.${compose_project_name}_default.IPAddress }}" "${compose_project_name}-${intermediary_host}-connector-1")"

common_curl_opts=(-sS --fail -H "Content-Type: application/json")

info Starting pipeline deployment
start_pipeline_args=("${initial_host}" "${initial_container_ip}" "${intermediary_host}" "${intermediary_container_ip}" "${policy_file}")

if "$insecure"; then
  start_pipeline_args=(--no-atls "${start_pipeline_args[@]}")
fi

start_pipeline_output="$("./scripts/$start_script" "${start_pipeline_args[@]}" )"
read -r pipeline_host pipeline_id pipeline_port < <(echo "$start_pipeline_output")

stop_pipeline() {
  info "Stopping pipeline"
  curl -X DELETE "${common_curl_opts[@]}" "http://$pipeline_host:8080/api/v1.0/pipelines/$pipeline_id"
}
defer stop_pipeline

# Get the example data to send to the pipeline from the metadata contained in the compose file
example_data="$(yq -r '.["x-example-metadata"]["example-data"]' < "$compose_file" | sed "s|%PIPELINE_PORT%|${pipeline_port}|")"

info "Sending input data, this may take some time"
curl -sS --fail -d "$example_data" "http://${pipeline_port}/echo" >/dev/null &

await_container_logs() {
  local host="$1"
  local container="$2"
  local image="registry:5000/${container}:latest"
  local id

  for i in $(seq 60); do
    id="$(docker exec "${compose_project_name}-${host}-docker-1" docker ps --filter "ancestor=${image}" --format "{{ .ID }}" | head -n1)"
    if [[ -n "$id" ]]; then
        info "Attaching to ${host}/${container}"
        docker exec "${compose_project_name}-${host}-docker-1" docker logs -f "$id" 2>&1 | sed -u "s|^|[${host}/${container}] |"
        return
    fi

    [[ $i -eq 30 ]] && detail "Waiting for ${host}/${container} is taking an unexpectedly long time..."
    sleep 1
  done

  error "Timed out waiting for ${host}/${container}"
}

while IFS=$'\t' read -r host container; do
  await_container_logs "$host" "$container" &
done < <(yq -r '.["x-example-metadata"]["attach-to"][] | [.host, .container] | @tsv' < "$compose_file")

info "Attaching to selected pipeline containers"
detail "Press CTRL-C to detach"

# At this point, the user cancelling is the user exiting the script as intended.
# Returning the correct error makes the shell think so, too :)
trap 'exit 0' INT
wait
