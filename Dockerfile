FROM geonetwork:4.2.5
EXPOSE 8080
EXPOSE 8000

# Copy our jar to the lib folder so that scan can happens
COPY ./target/geonetwork4-1.0-SNAPSHOT.jar ./webapps/geonetwork/WEB-INF/lib
COPY ./target/dependency/* ./webapps/geonetwork/WEB-INF/lib
