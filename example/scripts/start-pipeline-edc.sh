#!/usr/bin/bash
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

require curl yq docker

if (( $# != 5 )); then
  echo "Usage: $0 <local connector> <local connector ip> <remote connector> <remote connector ip> <policy path>" >&2
  exit 1
fi

edc_host="$2"
gc_host="$3"
gc_ip="$4"
policy="$5"

edc_mgmt_url="http://$edc_host:19193/management/v3"
common_curl_opts=(
  -sS
  --fail
  -H "Content-Type: application/json"
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

wait_for_negotiation() {
  local negotiation_id="$1"
  local timeout="${2:-30}"
  local negotiation state

  echo "Waiting ${timeout}s for negotiation completion" >&2
  # shellcheck disable=SC2034
  for i in $(seq "$timeout") ; do
      sleep 1
      negotiation="$(curl "$edc_mgmt_url/contractnegotiations/$negotiation_id")"
      state="$(echo "$negotiation" | yq -r ".state")"
      case "$state" in
        FINALIZED)
          echo "Negotiation completed successfully" >&2
          return 0
          ;;
        TERMINATED)
          echo "Negotiation failed" >&2
          return 1
          ;;
        *)
          ;;
      esac
  done

  echo "Negotiation timed out"
  return 1
}

get_hash "http-echo" "HTTP_ECHO_HASH"

echo "Waiting for the EDC to start up..." >&2
until curl "${common_curl_opts[@]}" -X POST "$edc_mgmt_url/contractnegotiations/request" > /dev/null; do
  sleep 0.5
done

policy_content="$(GC_ADDRESS="http://$gc_host:8444" envsubst < "$policy")"

echo "Starting the contract negotiation..." >&2
negotiation_id="$(curl "${common_curl_opts[@]}" -d "$policy_content" "$edc_mgmt_url/contractnegotiations" | yq -r '.["@id"]')"

echo "Awaiting negotiation completion..." >&2
wait_for_negotiation "$negotiation_id" || exit $?

echo "Getting contract agreement ID..." >&2
contract_id="$(curl "${common_curl_opts[@]}" "$edc_mgmt_url/contractnegotiations/$negotiation_id" | yq -r '.contractAgreementId')"

# From this point, we talk directly to the GLUE connector since the EDC does not have a control plane for our pipelines

target="$(echo "$policy_content" | yq -r '.policy.target["@id"]')"
action="$(echo "$policy_content" | yq -r '.policy.permission["odrl:action"]')"

echo "Starting pipeline on target GLUE connector..." >&2
pipeline_req="$(jo contractId="$contract_id" target="$target" action="$action")"
# Note: We purposefully use the inter-connector-interface here instead of the user API
pipeline_resp="$(curl "${common_curl_opts[@]}" -d "$pipeline_req" "http://$gc_ip:8444/pipeline")"

pipeline_id="$(yq -r '.pipelineName' <<< "$pipeline_resp")"
pipeline_port="$(yq -r '.ports[0].hostAddress' <<< "$pipeline_resp")"

echo "Pipeline running on $gc_ip" >&2
echo "$gc_ip $pipeline_id $pipeline_port"