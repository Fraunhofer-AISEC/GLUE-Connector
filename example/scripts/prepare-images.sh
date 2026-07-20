#!/bin/bash

set -euo pipefail

out_dir="${1:-images}"
mkdir -p "$out_dir"

destination_registry="registry:5000"
source_registry="${SOURCE_REGISTRY:-container-registry.gitlab.cc-asp.fraunhofer.de/ccit-glue/usage-control/container-pipeline}"
image_tag="${SOURCE_TAG:-running-example}"

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

require skopeo

copy_image() {
  local image_name="$1"
  local source_ref="docker://${source_registry}/${image_name}:${image_tag}"
  local target_ref="${destination_registry}/${image_name}:latest"
  local tar_path="${out_dir}/${image_name}.tar"

  skopeo copy --multi-arch all --insecure-policy "$source_ref" "oci-archive:${tar_path}:${target_ref}"
}

copy_image "container-pipeline"
copy_image "http-echo"
copy_image "proxy"
copy_image "atls-proxy"
