#!/bin/bash
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"


IMAGE_NAME=cavemandaveman/nexus
IMAGE_VERSION=${1:-3.16.2-01}

echo "Build ${IMAGE_NAME}:${IMAGE_VERSION} ..."
docker build --build-arg nexus_version=${IMAGE_VERSION} \
             -t ${IMAGE_NAME}:${IMAGE_VERSION} \
             -f "${DIR}/Dockerfile" \
             "${DIR}"
