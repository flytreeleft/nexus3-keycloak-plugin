#!/bin/bash
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"

NEXUS_HOME=/opt/sonatype/nexus
KEYCLOAK_CONFIG="${DIR}/keycloak.json"

DCR_NAME=nexus3
DCR_IMAGE=nexus-oss/nexus3
DCR_IMAGE_VERSION=3.6.0-02-r3

if [ ! -e "${KEYCLOAK_CONFIG}" ]; then
    echo "Please provide your keycloak.json and put it to ${DIR}"
    exit 1
fi

docker run -d --name ${DCR_NAME} \
                --restart always \
                --ulimit nofile=655360 \
                -e NEXUS_CONTEXT="/" \
                -e JAVA_MAX_MEM=4096M \
                -v "${KEYCLOAK_CONFIG}":${NEXUS_HOME}/etc/keycloak.json:ro \
                -p 8081:8081 \
                -p 5000:5000 \
                ${DCR_IMAGE}:${DCR_IMAGE_VERSION}
