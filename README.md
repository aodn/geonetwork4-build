# GeoNetwork 4 - Docker Image for AODN

This repo is used to customize a GeoNetwork4 to be used by AODN. The key customization is to
build a jar file with the code we want to add and insert it use COPY function to copy it to the lib
folder during the Dockerfile build.

The jar file contains a hack where we name a @Configuration class with the same package as
geonetwork4 base package (org.fao.geonet), so that the initial component-scan will pick up this
class. From there we add additional component-scan to our custom classes. This avoided the need
to alter the xml like what we did before plus we are using a Docker base image of GeoNetwork4.

## Run locally
You need create a file call .env and put in the following attribute if you do not want the
default startup parameters

```shell
ES_HOST=ec2-3-25-64-248.ap-southeast-2.compute.amazonaws.com
INDEXER_HOST=ec2-3-25-163-152.ap-southeast-2.compute.amazonaws.com
INDEXER_PORT=8081
INDEXER_APIKEY=THE_API_KEY_TO_CALL_INDEXER

# By default it runs in the h2 db, you can use postgis + postgres, so below config optional
GEONETWORK_DB_TYPE=postgres-postgis
GEONETWORK_DB_PORT=5432
GEONETWORK_DB_NAME=geonetwork
```

Then assume you have installed docker and docker-compose, then you can run ./startEsLocal.sh to run a elastic search 7 for
use by the geonetwork. It takes a while to start (3 mins+), so you can check http://localhost:5601 if system
started. Once started you can run

```shell
# Start geonetwork4
docker-compose -f docker-gn-compose.yml up --build

# Stop geonetwork4
docker-compose -f docker-gn-compose.yml down -v
```

Once elastic started you can run ./startGn4Local.sh to start the geonetwork. It is recommend to start
it like this because it will rebuild your images with the binary that you created from maven build install

## Ssh to instance
You can login to the geonetwork4 instance to debug you setting by
```shell
docker exec -it geonetwork4 /bin/bash
```

Once you in the shell, you can go to /var/lib/jetty/webapps/geonetwork

## Actuator
We have incorporate actuator to the instance, and you can visit to see the supported endpoints, there is one
issue is that it is running with ECS then you may not hit the same instance as you want.

http://localhost:8080/geonetwork/srv/api/manage

Geonetwork4 should have config to be a single instance only to avoid this issue. You can use the logfile
endpoint to view the log file directly as cloud watch is not so easy to use.

### Endpoints:

| Description  | Endpoints                            | Environment |
|--------------|--------------------------------------|-------------|
| Logfile      | `/geonetwork/srv/api/manage/logfile` | Edge        |
| Beans info   | `/geonetwork/srv/api/manage/beans`   | Edge        |
| Env info     | `/geonetwork/srv/api/manage/env`     | Edge        |
| Info         | `/geonetwork/srv/api/manage/info`    | Edge        |
| Health check | `/geonetwork/srv/api/manage/health`  | Edge        |
| Setup        | `/geonetwork/srv/api/setup`          | Edge        |

## Schema folder

The schema folder contains an open-api schema file from Genetwork4, you can get it from any instance
of GN4 (http://domain_name/geonetwork/srv/api/doc)

Once you have the json, you can generate code like the one here in Java to access GN4 via API.

## Use of S3

You can see a config file related to S3, however we do not use it because after experiment it, it
didn't support well as the GN4 will issue warning on file not found with relative folder name. The
code is just keep as a record.
