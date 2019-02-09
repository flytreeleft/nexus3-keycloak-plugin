# https://github.com/cavemandaveman/nexus
FROM cavemandaveman/nexus:3.6.0-02

ENV NEXUS_PLUGINS ${NEXUS_HOME}/system

# https://github.com/flytreeleft/nexus3-keycloak-plugin
ENV KEYCLOAK_PLUGIN_VERSION 0.3.2-SNAPSHOT
ENV KEYCLOAK_PLUGIN org.github.flytreeleft/nexus3-keycloak-plugin/${KEYCLOAK_PLUGIN_VERSION}

ADD https://github.com/flytreeleft/nexus3-keycloak-plugin/releases/download/${KEYCLOAK_PLUGIN_VERSION}/nexus3-keycloak-plugin-${KEYCLOAK_PLUGIN_VERSION}.jar \
    ${NEXUS_PLUGINS}/org/github/flytreeleft/nexus3-keycloak-plugin/${KEYCLOAK_PLUGIN_VERSION}/nexus3-keycloak-plugin-${KEYCLOAK_PLUGIN_VERSION}.jar

RUN chmod 644 ${NEXUS_PLUGINS}/org/github/flytreeleft/nexus3-keycloak-plugin/${KEYCLOAK_PLUGIN_VERSION}/nexus3-keycloak-plugin-${KEYCLOAK_PLUGIN_VERSION}.jar
RUN echo "mvn\\:${KEYCLOAK_PLUGIN} = 200" >> ${NEXUS_HOME}/etc/karaf/startup.properties
