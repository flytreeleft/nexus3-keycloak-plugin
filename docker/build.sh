#!/bin/bash
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"

IMAGE_NAME=nexus-oss/nexus3
IMAGE_VERSION=3.6.0-02-r3

docker build -t ${IMAGE_NAME}:${IMAGE_VERSION} \
             -f "${DIR}/Dockerfile" "${DIR}"
