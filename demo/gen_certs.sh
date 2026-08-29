#!/usr/bin/env bash
# Generate a demo CA + server certificate for https://10.0.2.2:8443
set -euo pipefail
OUT="${1:-.}"
cd "$OUT"

if [ ! -f ca.pem ]; then
  openssl req -x509 -newkey rsa:2048 -keyout ca.key -out ca.pem -days 30 -nodes \
    -subj "/CN=DigiVas Demo CA" >/dev/null 2>&1
fi

if [ ! -f server.crt ]; then
  openssl req -newkey rsa:2048 -keyout server.key -out server.csr -nodes \
    -subj "/CN=10.0.2.2" >/dev/null 2>&1
  printf "subjectAltName=IP:10.0.2.2,DNS:localhost\nextendedKeyUsage=serverAuth\n" > ext.cnf
  openssl x509 -req -in server.csr -CA ca.pem -CAkey ca.key -CAcreateserial \
    -out server.crt -days 30 -extfile ext.cnf >/dev/null 2>&1
fi

HASH=$(openssl x509 -in ca.pem -subject_hash_old -noout)
cp ca.pem "ca-$HASH.0"
echo "HASH=$HASH"