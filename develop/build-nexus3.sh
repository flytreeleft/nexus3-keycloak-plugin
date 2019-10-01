#!/bin/bash
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"


BASE_IMAGE_VERSION=3.16.2-01
bash "${DIR}/cavemandaveman-nexus-docker/build.sh" ${BASE_IMAGE_VERSION}


IMAGE_NAME=nexus3-keycloak-dev
IMAGE_VERSION=${BASE_IMAGE_VERSION}

echo "Build ${IMAGE_NAME}:${IMAGE_VERSION} ..."
docker build -t ${IMAGE_NAME}:${IMAGE_VERSION} \
             -f "${DIR}/Dockerfile" "${DIR}"
