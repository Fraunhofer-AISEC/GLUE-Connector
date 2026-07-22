#!/bin/bash

set -euo pipefail

cd "$(dirname "$0")"

for values in *-values.yaml; do
  ./render-template.py compose.yml.j2 "$values" > "../${values%-values.yaml}.compose.yml"
  ./render-template.py --insecure compose.yml.j2 "$values" > "../${values%-values.yaml}-insecure.compose.yml"
done