#!/bin/bash
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"

DCR_NAME=keycloak-dev
DCR_IMAGE_NAME=jboss/keycloak
DCR_IMAGE_VERSION=4.5.0.Final
DCR_IMAGE="${DCR_IMAGE_NAME}:${DCR_IMAGE_VERSION}"

DCR_DATA_VOLUME="${DIR}/develop/data/keycloak"

echo "Remove the existing docker container - ${DCR_NAME}"
docker rm -f ${DCR_NAME}

echo "Change the owner of the data volume - ${DCR_DATA_VOLUME}"
docker run --rm \
                -u root \
                -v "${DCR_DATA_VOLUME}":/mnt \
                --entrypoint /bin/chown \
                -it ${DCR_IMAGE} \
                -R jboss /mnt

echo "Create new docker container - ${DCR_NAME}"
docker run -d --name ${DCR_NAME} \
                --restart always \
                -e KEYCLOAK_USER=admin \
                -e KEYCLOAK_PASSWORD=admin123 \
                -v "${DCR_DATA_VOLUME}":/opt/jboss/keycloak/standalone/data \
                -p 8086:8080 \
                ${DCR_IMAGE}
