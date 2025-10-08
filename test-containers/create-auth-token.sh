#!/usr/bin/env bash
set -euo pipefail

# Prompt for username with default
read -r -p "Username [container-token]: " username
username=${username:-container-token}

# Prompt for password (input hidden)
read -r -s -p "Password: " password
echo

# Build JSON with jo
json=$(jo username="$username" password="$password" email="" serveraddress="container-registry.gitlab.cc-asp.fraunhofer.de")

# Base64 encode without wrapping or trailing newline
encoded=$(printf '%s' "$json" | base64 -w 0)

printf '%s' "$encoded"
