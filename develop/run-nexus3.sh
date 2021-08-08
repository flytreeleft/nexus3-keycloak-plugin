#!/bin/bash
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"

NEXUS_HOME=/opt/sonatype/nexus

DCR_NAME=nexus3-dev
DCR_IMAGE_NAME=nexus3-keycloak-dev
DCR_IMAGE_VERSION=3.16.2-01
DCR_IMAGE="${DCR_IMAGE_NAME}:${DCR_IMAGE_VERSION}"

DCR_DATA_VOLUME="${DIR}/data/nexus3-${DCR_IMAGE_VERSION}"

PLUGIN_VERSION=x.y.z-dev
PLUGIN_JAR="$(ls "${DIR}/../target/nexus3-keycloak-plugin"-*-SNAPSHOT.jar)"

keycloak_json_mappings=""
for file in `ls "${DIR}"/keycloak*.json`; do
    name=$(basename "${file}")
    keycloak_json_mappings="${keycloak_json_mappings} -v ${file}:${NEXUS_HOME}/etc/${name}:ro"
done

echo "Remove the existing docker container - ${DCR_NAME}"
docker rm -f ${DCR_NAME}

echo "Change the owner of the data volume - ${DCR_DATA_VOLUME}"
docker run --rm \
                -u root \
                -v "${DCR_DATA_VOLUME}":/mnt \
                --entrypoint /bin/chown \
                ${DCR_IMAGE} \
                -R nexus /mnt

echo "Create new docker container - ${DCR_NAME}"
docker run -d --name ${DCR_NAME} \
                -e NEXUS_CONTEXT="/" \
                -v "${DCR_DATA_VOLUME}":/nexus-data \
                ${keycloak_json_mappings} \
                -v "${PLUGIN_JAR}":"${NEXUS_HOME}/system/org/github/flytreeleft/nexus3-keycloak-plugin/${PLUGIN_VERSION}/nexus3-keycloak-plugin-${PLUGIN_VERSION}.jar":ro \
                -p 172.17.0.1:8903:8081 \
                -p 172.17.0.1:5000:5000 \
                ${DCR_IMAGE}
