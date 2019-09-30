#!/bin/bash
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"


# https://github.com/vouch/vouch-proxy#running-from-docker
DCR_NAME=nexus3-vouch-proxy
DCR_IMAGE_NAME=voucher/vouch-proxy
DCR_IMAGE_VERSION=alpine
DCR_IMAGE="${DCR_IMAGE_NAME}:${DCR_IMAGE_VERSION}"

echo "Remove the existing docker container - ${DCR_NAME}"
docker rm -f ${DCR_NAME}

echo "Create new docker container - ${DCR_NAME}"
docker run -d --name ${DCR_NAME} \
                -p 172.17.0.1:9090:9090 \
                -v "${DIR}/vouch-proxy/config.yml":/config/config.yml \
                -v "${DIR}/data/vouch-proxy":/data \
                ${DCR_IMAGE}
