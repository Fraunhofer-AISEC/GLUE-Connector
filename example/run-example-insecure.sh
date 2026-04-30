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

require curl jq jo envsubst docker

peer_proto="https"
if [[ "$1" == "--no-atls" ]]; then
  peer_proto="http"
  shift
fi

if (( $# != 3 )); then
  echo "Usage: $0 <local connector> <remote connector> <policy path>" >&2
  exit 1
fi

local_connector_name="$1"
remote_connector="$2"
policy="$3"

# As the connectors are running in containers, we cannot rely on DNS to resolve their names.
# If we for some reason want to use DNS in the future (e.g. for TLS), we may need to deploy a "user agent" container and use docker exec to run the script.
# Alternatively, we can just rely on the UI when its done.
local_connector_ip="$(docker inspect -f '{{ .NetworkSettings.Networks.example_default.IPAddress }}' "example-$local_connector_name-connector-1")"

common_curl_opts=(
  -sS
  --fail
  -H "Content-Type: application/json"
)

REGISTRY="container-registry.gitlab.cc-asp.fraunhofer.de/ccit-glue/usage-control/container-pipeline"

pull_and_get_hash() {
    local image_name="$1"
    local var_name="$2"

    local image_path="${REGISTRY}/${image_name}:running-example"
    # shellcheck disable=SC2155
    local image=$(docker pull -q "$image_path")

    # Get the canonical digest from the registry (manifest list)
    # shellcheck disable=SC2155
    local image_digest=$(docker inspect --format='{{index .RepoDigests 0}}' "$image_path" | cut -d'@' -f2)

    # Export the digest with the provided variable name
    export "$var_name"="$image_digest"

    printf "Found digest of %s:\n%s\n" "$image" "$image_digest"
}

# Pull images and get their hashes
pull_and_get_hash "http-echo" "HTTP_ECHO_HASH"
pull_and_get_hash "proxy" "PROXY_HASH"
pull_and_get_hash "forwarder" "FORWARDER_HASH"
pull_and_get_hash "atls-proxy" "ATLS_PROXY_HASH"

echo "Waiting for local connector at http://$local_connector_ip:8080 to become available..."
until curl -sS "http://$local_connector_ip:8080" > /dev/null 2>&1; do sleep 0.1; done

# Prepare CA certificate for insertion. Temporary workaround until we use app config for this.
# shellcheck disable=SC2155
if [ -f /var/ca.pem ]; then
  export CA_CERT="$(sed -z 's/\n/\\n/g' /var/ca.pem)"
fi

policy_content="$(envsubst < "$policy")"
# shellcheck disable=SC2016
local_pipeline_def="$(envsubst <<<'{
  "@type": "http://fhg.de/glue/ContainerPipeline",
  "http://fhg.de/glue/atlsProxyOut": {
    "@type": "http://fhg.de/glue/AtlsProxyConfiguration",
    "http://fhg.de/glue/proxyMode": "client",
    "http://fhg.de/glue/proxyEndpoint": [
      {
        "@type": "http://fhg.de/glue/AtlsProxyEndpoint",
        "http://fhg.de/glue/listenEndpoint": "0.0.0.0:8080",
        "http://fhg.de/glue/nextEndpoint": "intermediary:8444"
      }
    ]
  },
  "http://fhg.de/glue/container": [
    {
      "@type": "http://fhg.de/glue/Container",
      "http://fhg.de/glue/image": "container-registry.gitlab.cc-asp.fraunhofer.de/ccit-glue/usage-control/container-pipeline/proxy:running-example",
      "http://fhg.de/glue/hash": "${PROXY_HASH}",
      "http://fhg.de/glue/allowNetwork": true
    }, {
      "@type": "http://fhg.de/glue/Container",
      "http://fhg.de/glue/image": "container-registry.gitlab.cc-asp.fraunhofer.de/ccit-glue/usage-control/container-pipeline/forwarder:running-example",
      "http://fhg.de/glue/hash": "${FORWARDER_HASH}",
      "http://fhg.de/glue/allowNetwork": true,
      "http://fhg.de/glue/parameter": {
        "@type": "http://fhg.de/glue/RestParameter",
        "http://fhg.de/glue/httpUrl": "intermediary:8444"
      }
    }
  ]
}')"

echo "Starting the contract negotiation..."
negotiation_req="$(jo peer="$peer_proto://$remote_connector:8444" contractRequest="$policy_content" localPipeline="$local_pipeline_def")"
negotiation_id=$(curl -v -X POST "${common_curl_opts[@]}" -d "$negotiation_req" "http://$local_connector_ip:8080/api/v1.0/negotiations" | jq -r ".id")

echo "Awaiting negotiation completion..."
while true; do
  negotiation_state="$(curl "${common_curl_opts[@]}" "http://$local_connector_ip:8080/api/v1.0/negotiations/$negotiation_id" | jq -r ".state")"
  case "$negotiation_state" in
    FINALIZED)
      break
      ;;
    TERMINATED)
      echo "Negotiation was terminated with an error"
      exit 1
      ;;
  esac
  sleep 1s
done

echo "Requesting local pipelines..."
while (( "$(curl "${common_curl_opts[@]}" "http://$local_connector_ip:8080/api/v1.0/pipelines" | jq "length")" == 0 )); do
    sleep 1
done

# We once again only get the first pipeline for now.
pipeline="$(curl "${common_curl_opts[@]}" "http://$local_connector_ip:8080/api/v1.0/pipelines" | jq '.[0]')"

pipeline_id="$(jq -r '.id' <<< "$pipeline")"
pipeline_port="$(jq -r '.["containers"][0]["ports"][0]["hostAddress"]' <<< "$pipeline")"

# Wait for the user to interrupt and then stop the pipeline
stop_pipeline() {
  echo "Stopping pipeline"
  curl -X DELETE "${common_curl_opts[@]}" "http://$local_connector_ip:8080/api/v1.0/pipelines/$pipeline_id"
  exit 0
}

trap stop_pipeline INT
echo "Container pipeline running on $pipeline_port (Press CTRL-C to stop)"

# This may not be the most elegant solution but it keeps the script responsive to both CTRL-C and injected SIGINTs
while true; do
  sleep 0.1s
done
