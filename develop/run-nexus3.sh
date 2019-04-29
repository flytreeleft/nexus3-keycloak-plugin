#!/bin/bash
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"

NEXUS_HOME=/opt/sonatype/nexus

DCR_NAME=nexus3-keycloak-dev
DCR_IMAGE_NAME=nexus3-keycloak-dev
DCR_IMAGE_VERSION=3.6.0-02
DCR_IMAGE="${DCR_IMAGE_NAME}:${DCR_IMAGE_VERSION}"

DCR_DATA_VOLUME="${DIR}/data/nexus3"

PLUGIN_VERSION=x.y.z-dev
PLUGIN_JAR="$(ls "${DIR}/../target/nexus3-keycloak-plugin"-*-SNAPSHOT.jar)"

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
                -e NEXUS_CONTEXT="/" \
                -v "${DCR_DATA_VOLUME}":/nexus-data \
                -v "${DIR}/keycloak.json":${NEXUS_HOME}/etc/keycloak.json:ro \
                -v "${DIR}/keycloak.0.json":${NEXUS_HOME}/etc/keycloak.0.json:ro \
                -v "${PLUGIN_JAR}":"${NEXUS_HOME}/system/org/github/flytreeleft/nexus3-keycloak-plugin/${PLUGIN_VERSION}/nexus3-keycloak-plugin-${PLUGIN_VERSION}.jar":ro \
                -p 172.17.0.1:8903:8081 \
                ${DCR_IMAGE}
