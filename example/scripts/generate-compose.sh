#!/bin/bash
set -euo pipefail

this_dir="$(dirname "$0")"
template_dir="$this_dir/../compose-generator"

cd "$template_dir"

# Render into a temporary file first so that a failed render does not leave a truncated compose file behind
trap 'rm -f ../*.compose.yml.tmp' EXIT

render() {
  local target="$1"
  shift
  ./render-template.py "$@" > "$target.tmp"
  mv "$target.tmp" "$target"
}

for values in *-values.yaml; do
  render "../${values%-values.yaml}.compose.yml" compose.yml.j2 "$values"
  render "../${values%-values.yaml}-insecure.compose.yml" --insecure compose.yml.j2 "$values"
done
