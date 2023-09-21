# GeoNetwork 4 - Docker Image for AODN

This repo is used to customize a GeoNetwork4 to be used by AODN. The key customization is to
build a jar file with the code we want to add and insert it use COPY function to copy it to the lib
folder during the Dockerfile build.

The jar file contains a hack where we name a @Configuration class with the same package as 
geonetwork4 base package (org.fao.geonet), so that the initial component-scan will pick up this
class. From there we add additional component-scan to our custom classes. This avoided the need
to alter the xml like what we did before plus we are using a Docker base image of GeoNetwork4.

## Schema folder

The schema folder contains an open-api schema file from Genetwork4, you can get it from any instance
of GN4 (http://domain_name/geonetwork/srv/api/doc)

Once you have the json, you can generate code like the one here in Java to access GN4 via API.

## Use of S3

You can see a config file related to S3, however we do not use it because after experiment it, it 
didn't support well as the GN4 will issue warning on file not found with relative folder name. The
code is just keep as a record. 
