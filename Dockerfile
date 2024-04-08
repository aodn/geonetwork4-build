FROM geonetwork:4.4.1
EXPOSE 8080
EXPOSE 8000

# When doing upgrade check https://github.com/geonetwork/docker-geonetwork/blob/main/<version>/Dockerfile to see
# if they change the root directory location.
ENV GN_DIR /opt/geonetwork

# How do Docker volumes stack up against bind mounts? While both aim to persist data, there are notable differences.
#
# Bind mounts have been a part of Docker since its inception. They depend on the host machine’s filesystem to store
# data and can be located anywhere on the host system. Use volume in docker composes
#

# Override log4j as we need to add our log appear, by default ROOT level is off which is very strange design
COPY ./geonetwork/target/classes/log4j-imos.xml ${GN_DIR}/WEB-INF/classes/log4j2.xml

# Copy our jar to the lib folder so that scan can happens
COPY ./geonetwork/target/geonetwork-*.jar ${GN_DIR}/WEB-INF/lib/
COPY ./geonetwork-api/target/geonetwork-api-*.jar ${GN_DIR}/WEB-INF/lib/

COPY ./geonetwork/target/dependency/* ${GN_DIR}/WEB-INF/lib/
COPY ./geonetwork/target/classes/schema_plugins/converter/*.xsl ${GN_DIR}/WEB-INF/data/config/schema_plugins/iso19139/process/
COPY ./geonetwork/target/classes/schema_plugins/converter/*.xsl ${GN_DIR}/WEB-INF/data/config/schema_plugins/iso19115-3.2018/process/

# Config override
COPY ./geonetwork/target/classes/gnconfig/config-overrides.xml ${GN_DIR}/WEB-INF/
