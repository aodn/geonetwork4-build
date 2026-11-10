FROM geonetwork:4.4.12
EXPOSE 8080
EXPOSE 8000

# When doing upgrade check https://github.com/geonetwork/docker-geonetwork/blob/main/<version>/Dockerfile to see
# if they change the root directory location.
ENV GN_DIR=/opt/geonetwork

# How do Docker volumes stack up against bind mounts? While both aim to persist data, there are notable differences.
#
# Bind mounts have been a part of Docker since its inception. They depend on the host machine’s filesystem to store
# data and can be located anywhere on the host system. Use volume in docker composes
#

# Override log4j as we need to add our log appear, by default ROOT level is off which is very strange design
COPY ./geonetwork-core/target/classes/log4j-imos.xml ${GN_DIR}/WEB-INF/classes/log4j2.xml
COPY ./geonetwork-core/target/classes/log4j-imos-index.xml ${GN_DIR}/WEB-INF/classes/log4j2-index.xml

# Copy our jar to the lib folder so that scan can happens
COPY ./geonetwork-core/target/geonetwork4-*.jar ${GN_DIR}/WEB-INF/lib/
COPY ./geonetwork-api/target/geonetwork-api-*.jar ${GN_DIR}/WEB-INF/lib/

COPY ./geonetwork-core/target/dependency/* ${GN_DIR}/WEB-INF/lib/
COPY ./geonetwork-core/target/classes/schema_plugins/converter/*.xsl ${GN_DIR}/WEB-INF/data/config/schema_plugins/iso19139/process/
COPY ./geonetwork-core/target/classes/schema_plugins/converter/*.xsl ${GN_DIR}/WEB-INF/data/config/schema_plugins/iso19115-3.2018/process/

# Config override
COPY ./geonetwork-core/target/classes/gnconfig/config-overrides.xml ${GN_DIR}/WEB-INF/

# Labels for our own settings in Admin console > Settings, en-custom.json is GeoNetwork's hook for custom translations
COPY ./geonetwork-core/target/classes/gnconfig/en-custom.json ${GN_DIR}/catalog/locales/en-custom.json

# ECS runs this container as uid 999 (the old Jetty user, and the EFS access point).
# 4.4.12 is Tomcat and its entrypoint sed -i writes under /usr/local/tomcat/conf.
# That directory is root-owned, so the task exits immediately unless 999 can write it.
# chown after the COPY lines so the files added above are included.
RUN groupadd -g 999 geonetwork \
 && useradd -u 999 -g 999 -m -d /home/geonetwork -s /bin/sh geonetwork \
 && chown -R 999:999 /usr/local/tomcat/conf /opt/geonetwork
USER 999
