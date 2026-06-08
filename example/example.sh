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
          printf "  --build           Rebuild all container images\n"
          printf "  --initial         The name of the initial container (default: initiator)\n"
          printf "  --intermediary    The name of the first contract recipient container (default: intermediary)\n"
          printf "  --initial         The name of the final container (default: destination)\n"
          printf "  --help            Show this message and exit\n"
}

# Simple Go-style exit handler stack

_defer_handlers=()

defer() {
    _defer_handlers+=("$*")
}

_run_deferred() {
    for (( i=${#_defer_handlers[@]}-1; i>=0; i-- )); do
        eval "${_defer_handlers[$i]}"
    done
}
trap _run_deferred EXIT


require curl jq zstd docker

insecure=false
regenerate_pki=false
build=false
# TODO: It would be cleaner to mark this in the compose.yml files somehow
initial_host=initiator
intermediary_host=intermediary
final_host=destination

while [[ $# -gt 0 ]]; do
    case $1 in
      --insecure)
        insecure=true
        ;;
      --regenerate-pki)
        regenerate_pki=true
        ;;
      --build)
        build=true
        ;;
      --initial)
        initial_host="$2"
        shift
        ;;
      --intermediary)
        intermediary_host="$1"
        shift
        ;;
      --final)
        final_host="$2"
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
    shift
done

if [[ $# -eq 0 ]]; then
  print_help >&2
  exit 1
fi

compose_file="$1$($insecure && echo "-insecure").compose.yml"
policy_file="$1.policy.json"

if [ ! -f "${compose_file}" ] || [ ! -f "${policy_file}" ]; then
  echo "Example $1 does not exist" >&2
  exit 1
fi

if [ ! -d pki ] || ${regenerate_pki}; then
    info "Generating new PKI certificates"
    ./scripts/generate-pki.sh
fi

if ${build}; then
    info "Building container images"
    # TODO: The example should probably work standalone, so don't rely on building stuff after testing is done
    (cd .. && ./gradlew jibDockerBuild)
    docker compose -f "${compose_file}" build
fi

info "Starting docker compose environment"

# TODO: Support log output somehow?
docker compose -f "${compose_file}" up -d

remove_compose() {
  info "Removing docker compose environment"
  docker compose -f "${compose_file}" down -v
}
defer remove_compose

initial_container_ip="$(docker inspect -f '{{ .NetworkSettings.Networks.example_default.IPAddress }}' "example-${initial_host}-connector-1")"
common_curl_opts=(-sS --fail -H "Content-Type: application/json")

info Starting pipeline deployment
start_pipeline_output="$(./scripts/start-pipeline.sh "$($insecure && echo "--no-atls")" "${initial_container_ip}" "${intermediary_host}" "${policy_file}")"
read -r pipeline_id pipeline_port < <(echo "$start_pipeline_output")

stop_pipeline() {
  info "Stopping pipeline"
  curl -X DELETE "${common_curl_opts[@]}" "http://$initial_container_ip:8080/api/v1.0/pipelines/$pipeline_id"
}
defer stop_pipeline

example_data=">>=======================================================<<
||                                                       ||
||     _   _      _ _          ____ _    _   _ _____     ||
||    | | | | ___| | | ___    / ___| |  | | | | ____|    ||
||    | |_| |/ _ \ | |/ _ \  | |  _| |  | | | |  _|      ||
||    |  _  |  __/ | | (_) | | |_| | |__| |_| | |___     ||
||    |_| |_|\___|_|_|\___/   \____|_____\___/|_____|    ||
||                                                       ||
>>=======================================================<<

A full data pipeline is now established.
You can send more data through the pipeline by sending HTTP
requests to <http://${pipeline_port}/echo>.
"

info "Sending input data, this may take some time"
curl -sS --fail -d "$example_data" "http://${pipeline_port}/echo" >/dev/null &

echo_container_image="container-registry.gitlab.cc-asp.fraunhofer.de/ccit-glue/usage-control/container-pipeline/http-echo:running-example"
iteration=0
while true; do
  echo_container_id="$(docker exec "example-${final_host}-docker-1" docker ps --filter "ancestor=$echo_container_image" --format "{{ .ID }}" | head -n1)"

  if [ -n "${echo_container_id}" ]; then
    break
  fi

  iteration=$((iteration + 1))
  if [[ $iteration -eq 30 ]]; then
      detail "The initial message is taking an unexpectedly long time..."
  elif [[ $iteration -eq 60 ]]; then
      error "Initial pipeline message timed out, exiting"
      kill %1
      exit 1
  fi

  sleep 1
done

info "Attaching to the final connector in the pipeline"
detail "Press CTRL-C to detach"

docker exec "example-${final_host}-docker-1" docker logs -f "${echo_container_id}"

wait %1
