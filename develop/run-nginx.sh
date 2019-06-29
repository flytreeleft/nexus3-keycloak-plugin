#!/bin/bash
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"


# https://github.com/flytreeleft/docker-nginx-gateway
DCR_NAME=nexus3-nginx-gateway
DCR_IMAGE_NAME=flytreeleft/nginx-gateway
DCR_IMAGE_VERSION=latest
DCR_IMAGE="${DCR_IMAGE_NAME}:${DCR_IMAGE_VERSION}"

echo "Remove the existing docker container - ${DCR_NAME}"
docker rm -f ${DCR_NAME}

echo "Create new docker container - ${DCR_NAME}"
docker run -d --name ${DCR_NAME} \
                -p 172.17.0.1:80:80 \
                -e DISABLE_CERTBOT=true \
                -e DISABLE_GIXY=true \
                -v "${DIR}/vhost.d":/etc/nginx/vhost.d \
                ${DCR_IMAGE}
