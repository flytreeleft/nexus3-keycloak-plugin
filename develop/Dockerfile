FROM cavemandaveman/nexus:3.6.0-02

ENV NEXUS_PLUGINS ${NEXUS_HOME}/system

# Change the real plugin version to `x.y.z-dev`,
# so that, we can keep the building scripts
# without changes for different version.
ENV KEYCLOAK_PLUGIN_VERSION x.y.z-dev
ENV KEYCLOAK_PLUGIN org.github.flytreeleft/nexus3-keycloak-plugin/${KEYCLOAK_PLUGIN_VERSION}

RUN mkdir -p ${NEXUS_PLUGINS}/org/github/flytreeleft/nexus3-keycloak-plugin/${KEYCLOAK_PLUGIN_VERSION}/ \
        && echo "mvn\\:${KEYCLOAK_PLUGIN} = 200" >> ${NEXUS_HOME}/etc/karaf/startup.properties
