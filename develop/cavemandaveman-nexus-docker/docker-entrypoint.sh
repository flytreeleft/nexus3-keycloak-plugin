#!/bin/sh

set -x

if [ "$1" = 'bin/nexus' ]; then

  if [ -f "${NEXUS_DATA}/keystore.jks" ]; then
    ln -s "${NEXUS_DATA}/keystore.jks" "${NEXUS_HOME}/etc/ssl/keystore.jks"
    sed \
      -e "s|password</Set>|${JKS_PASSWORD}</Set>|g" \
      -i "${NEXUS_HOME}/etc/jetty/jetty-https.xml"
    sed \
      -e "s|nexus-args=.*|nexus-args=\${jetty.etc}/jetty.xml,\${jetty.etc}/jetty-http.xml,\${jetty.etc}/jetty-requestlog.xml,\${jetty.etc}/jetty-https.xml,\${jetty.etc}/jetty-http-redirect-to-https.xml|g" \
      -i "${NEXUS_HOME}/etc/nexus-default.properties"
    grep \
      -q "application-port-ssl" "${NEXUS_HOME}/etc/nexus-default.properties" || \
      sed \
        -e "\|application-port|a\application-port-ssl=8443" \
        -i "${NEXUS_HOME}/etc/nexus-default.properties"
  fi

  sed \
    -e "s|-Xms.*|-Xms${JAVA_MIN_MEM}|g" \
    -e "s|-Xmx.*|-Xmx${JAVA_MAX_MEM}|g" \
    -i "${NEXUS_HOME}/bin/nexus.vmoptions"

  if [ -d "${SONATYPE_WORK}/nexus3" ]; then
    rm -rf "${SONATYPE_WORK}/nexus3"
  fi

  mkdir -p "${NEXUS_DATA}/etc" "${NEXUS_DATA}/log" "${NEXUS_DATA}/tmp" "${SONATYPE_WORK}"
  ln -s "${NEXUS_DATA}" "${SONATYPE_WORK}/nexus3"
  chown -R nexus "${NEXUS_DATA}" "${SONATYPE_WORK}"
  exec su-exec nexus "$@"

fi

exec "$@"
