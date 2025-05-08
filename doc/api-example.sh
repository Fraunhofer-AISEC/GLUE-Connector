#!/bin/bash

set -euo pipefail

# This function just checks if all dependencies are available
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

require curl jq envsubst

if (( $# != 3 )); then
  echo "Usage: $0 <local connector> <remote connector> <policy path>" >&2
  exit 1
fi

local_connector="$1"
remote_connector="$2"
policy="$3"

common_curl_opts=(
  -sS
  --fail
  -H "Content-Type: application/json"
)

REGISTRY="container-registry.gitlab.cc-asp.fraunhofer.de/ccit-glue/usage-control/container-pipeline"

pull_and_get_hash() {
    local image_name="$1"
    local var_name="$2"

    local image_path="${REGISTRY}/${image_name}:latest"
    local image=$(docker pull -q "$image_path")
    local image_hash=$(docker images --no-trunc --quiet "$image_path")

    # Export the hash with the provided variable name
    export "$var_name"="$image_hash"

    printf "Found hash of %s:\n%s\n" "$image" "$image_hash"
}

# Pull images and get their hashes
pull_and_get_hash "http-echo" "HTTP_ECHO_HASH"
pull_and_get_hash "proxy" "PROXY_HASH"
pull_and_get_hash "forwarder" "FORWARDER_HASH"

policy_content="$(envsubst < "$policy")"

echo "Starting the contract negotiation..."
negotiation_req="$(jq '{"peer": "'"$remote_connector"'", "contractRequest": .}' <<< "$policy_content")"
curl -v -X POST "${common_curl_opts[@]}" -d "$negotiation_req" "http://$local_connector/api/negotiations"

echo "Requesting negotiated contracts..."
while (( "$(curl "${common_curl_opts[@]}" "http://$local_connector/api/negotiations" | jq "length")" != 0 )); do
  sleep 1s
done

# Parse out the relevant fields from the proposed contract (we just assume the negotiation was a success)
# Note: This assumes that the policy is not expanded and is using the same context names as in the example policy
# Also, note that this will just look at the first permission
contract_id="$(jq -r '.["@id"]' <<< "$policy_content")"
contract_target="$(jq -r '.["odrl:permission"][0]["odrl:target"]' <<< "$policy_content")"
contract_action="$(jq -r '.["odrl:permission"][0]["odrl:action"]' <<< "$policy_content")"

echo "Starting remote pipeline..."
pipeline_start_req='{"peer": "'"$remote_connector"'", "request": {"target": "'"$contract_target"'", "action": "'"$contract_action"'", "contractId": "'"$contract_id"'"}}'
pipeline_start_resp="$(curl -X POST "${common_curl_opts[@]}" -d "$pipeline_start_req" "http://$local_connector/api/remote-pipelines")"

pipeline_id="$(jq -r '.localId' <<< "$pipeline_start_resp")"
pipeline_port="$(jq -r '.remote.pipelinePort' <<< "$pipeline_start_resp")"

# Wait for the user to interrupt and then stop the remote pipeline
stop_pipeline() {
  echo "Stopping pipeline"
  curl -X DELETE "${common_curl_opts[@]}" "http://$local_connector/api/remote-pipelines/$pipeline_id"
  exit 0
}

trap stop_pipeline INT
echo "Container pipeline running on port $pipeline_port (Press CTRL-C to stop)"

# This may not be the most elegant solution but it keeps the script responsive to both CTRL-C and injected SIGINTs
while true; do
  sleep 0.1s
done
