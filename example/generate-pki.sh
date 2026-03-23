#!/bin/bash
set -euo pipefail

out_dir="${1:-pki}"

mkdir -p "$out_dir"

# CA Cert

openssl ecparam -name prime256v1 -genkey -noout -out "$out_dir/ca-key.pem"

openssl req -new -x509 -days 3650 -key "$out_dir/ca-key.pem" \
    -out "$out_dir/ca-cert.pem" \
    -subj "/C=DE/ST=Bavaria/L=Munich/O=Trusted Party/CN=Example Root CA" \
    -addext "basicConstraints=critical,CA:TRUE" \
    -addext "keyUsage=critical,keyCertSign,cRLSign" \
    -addext "subjectKeyIdentifier=hash"

# EST Server

openssl ecparam -name prime256v1 -genkey -noout -out "$out_dir/est-server-key.pem"

openssl req -new -key "$out_dir/est-server-key.pem" \
    -out "$out_dir/est-server-csr.pem" \
    -subj "/C=DE/ST=Bavaria/L=Munich/O=Trusted Party/CN=EST Server" \
    -addext "basicConstraints=CA:FALSE" \
    -addext "keyUsage=critical,digitalSignature,keyEncipherment" \
    -addext "extendedKeyUsage=serverAuth" \
    -addext "subjectAltName=DNS:estserver"

openssl x509 -req -in "$out_dir/est-server-csr.pem" \
    -CA "$out_dir/ca-cert.pem" -CAkey "$out_dir/ca-key.pem" \
    -CAcreateserial -out "$out_dir/est-server-cert.pem" -days 365 \
    -copy_extensions copy

rm "$out_dir/est-server-csr.pem"

openssl ecparam -name prime256v1 -genkey -noout -out "$out_dir/est-signing-key.pem"

openssl req -new -key "$out_dir/est-signing-key.pem" \
    -out "$out_dir/est-signing-csr.pem" \
    -subj "/C=DE/ST=Bavaria/L=Munich/O=Trusted Party/CN=EST Metadata Signer" \
    -addext "basicConstraints=CA:FALSE" \
    -addext "keyUsage=critical,digitalSignature"

openssl x509 -req -in "$out_dir/est-signing-csr.pem" \
    -CA "$out_dir/ca-cert.pem" -CAkey "$out_dir/ca-key.pem" \
    -CAcreateserial -out "$out_dir/est-signing-cert.pem" -days 365 \
    -copy_extensions copy

rm "$out_dir/est-signing-csr.pem"