FROM geonetwork:4.2.5
EXPOSE 8080
EXPOSE 8000

# How do Docker volumes stack up against bind mounts? While both aim to persist data, there are notable differences.
#
# Bind mounts have been a part of Docker since its inception. They depend on the host machine’s filesystem to store
# data and can be located anywhere on the host system.
#

# Copy our jar to the lib folder so that scan can happens
COPY ./target/geonetwork4-1.0-SNAPSHOT.jar ./webapps/geonetwork/WEB-INF/lib
COPY ./target/dependency/* ./webapps/geonetwork/WEB-INF/lib
