FROM geonetwork:4.2.5
EXPOSE 8080
EXPOSE 8000

# How do Docker volumes stack up against bind mounts? While both aim to persist data, there are notable differences.
#
# Bind mounts have been a part of Docker since its inception. They depend on the host machine’s filesystem to store
# data and can be located anywhere on the host system.
#

# Copy our jar to the lib folder so that scan can happens
COPY geonetwork/target/geonetwork4-*.jar ./webapps/geonetwork/WEB-INF/lib/
COPY geonetwork/target/dependency/* ./webapps/geonetwork/WEB-INF/lib/
COPY geonetwork/target/classes/schema_plugins/process/*.xsl ./webapps/geonetwork/WEB-INF/data/config/schema_plugins/iso19139/process/
COPY geonetwork/target/classes/schema_plugins/process/*.xsl ./webapps/geonetwork/WEB-INF/data/config/schema_plugins/iso19115-3.2018/process/

COPY geonetwork-api/target/geonetwork4-api-*.jar ./webapps/geonetwork/WEB-INF/lib/

# Config override
COPY geonetwork/target/classes/gnconfig/config-overrides.xml ./webapps/geonetwork/WEB-INF/
