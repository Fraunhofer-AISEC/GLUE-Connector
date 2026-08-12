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

require curl yq jo envsubst skopeo

peer_proto="https"
if [[ "$1" == "--no-atls" ]]; then
  peer_proto="http"
  shift
fi

if (( $# != 5 )); then
  echo "Usage: $0 <local connector> <local connector ip> <remote connector> <remote connector ip> <policy path>" >&2
  exit 1
fi

local_connector_ip="$2"
remote_connector="$3"
policy="$5"

common_curl_opts=(
  -sS
  --fail
  -H "Content-Type: application/json"
  # TODO: Put auth here when implemented
)

get_hash() {
    local image_name="$1"
    local var_name="$2"
    local image_ref="localhost:5000/${image_name}:latest"
    local image_hash
    image_hash=$(skopeo inspect --tls-verify=false "docker://${image_ref}" | yq -r '.Digest')

    # Export the hash with the provided variable name
    export "${var_name}"="${image_hash}"

    printf "Found digest of %s:\n%s\n" "${image_ref}" "${image_hash}" >&2
}

# Read image hashes from local archives
get_hash "http-echo" "HTTP_ECHO_HASH"
get_hash "proxy" "PROXY_HASH"
get_hash "atls-proxy" "ATLS_PROXY_HASH"

echo "Waiting for local connector at http://$local_connector_ip:8080 to become available..." >&2
until curl -sS "http://$local_connector_ip:8080" > /dev/null 2>&1; do sleep 0.1; done

# Prepare CA certificate for insertion. Temporary workaround until we use app config for this.
# shellcheck disable=SC2155
if [ -f /var/ca.pem ]; then
  export CA_CERT="$(sed -z 's/\n/\\n/g' /var/ca.pem)"
fi

policy_content="$(envsubst < "$policy")"
# TODO: Find a better way to specify the local pipeline.
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
      "http://fhg.de/glue/image": "registry:5000/proxy:latest",
      "http://fhg.de/glue/hash": "${PROXY_HASH}",
      "http://fhg.de/glue/allowNetwork": true
    }
  ]
}')"

echo "Starting the contract negotiation..." >&2
negotiation_req="$(jo peer="$peer_proto://$remote_connector:8444" contractRequest="$policy_content" localPipeline="$local_pipeline_def")"
negotiation_id=$(curl -v -X POST "${common_curl_opts[@]}" -d "$negotiation_req" "http://$local_connector_ip:8080/api/v1.0/negotiations" | yq -r ".id")

echo "Awaiting negotiation completion..." >&2
while true; do
  negotiation_state="$(curl "${common_curl_opts[@]}" "http://$local_connector_ip:8080/api/v1.0/negotiations/$negotiation_id" | yq -r ".state")"
  case "$negotiation_state" in
    FINALIZED)
      break
      ;;
    TERMINATED)
      echo "Negotiation was terminated with an error" >&2
      exit 1
      ;;
  esac
  sleep 1s
done

# TODO: Link this step to the negotiation in some form, either by storing the negotiated contract ID from the negotiation object or tagging the local pipelines.
echo "Requesting local pipelines..." >&2
while (( "$(curl "${common_curl_opts[@]}" "http://$local_connector_ip:8080/api/v1.0/pipelines" | yq "length")" == 0 )); do
    sleep 1
done

# We once again only get the first pipeline for now.
pipeline="$(curl "${common_curl_opts[@]}" "http://$local_connector_ip:8080/api/v1.0/pipelines" | yq '.[0]')"

pipeline_id="$(yq -r '.id' <<< "$pipeline")"
pipeline_port="$(yq -r '.["containers"][0]["ports"][0]["hostAddress"]' <<< "$pipeline")"

echo "Container pipeline running on $pipeline_port" >&2
echo "$local_connector_ip $pipeline_id $pipeline_port"
