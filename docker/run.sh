#!/bin/bash
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"

NEXUS_HOME=/opt/sonatype/nexus
KEYCLOAK_CONFIG="${DIR}/keycloak.json"

DCR_NAME=nexus3
DCR_IMAGE_NAME=nexus-oss/nexus3
DCR_IMAGE_VERSION=3.22.0
DCR_IMAGE="${DCR_IMAGE_NAME}:${DCR_IMAGE_VERSION}"
DCR_DATA_VOLUME="${DIR}/data/nexus3"

if [ ! -e "${KEYCLOAK_CONFIG}" ]; then
    echo "Please provide your keycloak.json and put it to ${DIR}"
    exit 1
fi

echo "Remove the existing docker container - ${DCR_NAME}"
docker rm -f ${DCR_NAME}

echo "Change the owner of the data volume - ${DCR_DATA_VOLUME}"
docker run --rm \
                -u root \
                -v "${DCR_DATA_VOLUME}":/mnt \
                --entrypoint /bin/chown \
                -it ${DCR_IMAGE} \
                -R nexus /mnt

echo "Create new docker container - ${DCR_NAME}"
docker run -d --name ${DCR_NAME} \
                --restart always \
                --ulimit nofile=655360 \
                -e NEXUS_CONTEXT="/" \
                -e JAVA_MAX_MEM=4096M \
                -v "${DCR_DATA_VOLUME}":/nexus-data \
                -v "${KEYCLOAK_CONFIG}":${NEXUS_HOME}/etc/keycloak.json:ro \
                -p 8081:8081 \
                ${DCR_IMAGE}
