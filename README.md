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

Assume you have installed docker and docker-compose, you can run ./startEsLocal.sh to run a elastic search 7 for
use by the geonetwork. It takes a while to start (3 mins+), so you can check http://localhost:5601 if system
started. Once started you can run

```shell
# If you run ./startEsLocal.sh, then you should point to localhost
ES_HOST=localhost

# We record changed, it will notify the indexer of update, check with dev on what is the value of API key
INDEXER_HOST=https://es-indexer-edge.aodn.org.au
INDEXER_PORT=443
INDEXER_APIKEY=THE_API_KEY_TO_CALL_INDEXER

# By default it runs in the h2 db, you can use postgis + postgres, so below config optional
GEONETWORK_DB_TYPE=postgres-postgis
GEONETWORK_DB_PORT=5432
GEONETWORK_DB_NAME=geonetwork

# Optional, by default use main branch to get the json config for GN4, however for development you may want to point
# to your own branch
GIT_BRANCH=xxx
```

```shell
# Start geonetwork4
docker-compose -f docker-gn-compose.yml up --build

# Stop geonetwork4
docker-compose -f docker-gn-compose.yml down -v
```

Once elastic started you can run ./startGn4Local.sh to start the geonetwork. It is recommend to start
it like this because it will rebuild your images with the binary that you created from maven build install

## Debug
If you run the geonetwork using ./startGn4Local.sh, then you can setup a debug profile using
```shell
-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:8000
```

and connect to the instance inside docker.

> Noted: You should run docker container prune and docker image prune periodically to free up disk space.

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

> You need to present X-XSRF-TOKEN in your header to call Setup endpoints, please read comments
> in [Api.java](./geonetwork/src/main/java/au/org/aodn/geonetwork4/controller/Api.java)

| Description                   | Method | Endpoints                                      | Param         | Environment   |
|-------------------------------|--------|------------------------------------------------|---------------|---------------|
| Logfile                       | GET    | `/geonetwork/srv/api/manage/logfile`           |               | Edge, Staging |
| Beans info                    | GET    | `/geonetwork/srv/api/manage/beans`             |               | Edge, Staging |
| Env info                      | GET    | `/geonetwork/srv/api/manage/env`               |               | Edge, Staging |
| Info                          | GET    | `/geonetwork/srv/api/manage/info`              |               | Edge, Staging |
| Health check                  | GET    | `/geonetwork/srv/api/manage/health`            |               | Edge, Staging |
| Read Record Misc Info         | GET    | `/geonetwork/srv/api/aodn/records/{uuid}/info` |               | Edge, Staging |
| Read Harvester - Config       | GET    | `/geonetwork/srv/api/aodn/setup/harvesters`    |               | Edge, Staging |
| Delete All Harvester - Config | DELETE | `/geonetwork/srv/api/aodn/setup/harvesters`    |               | Edge, Staging |
| Delete All Category - Config  | DELETE | `/geonetwork/srv/api/aodn/setup/categories`    |               | Edge, Staging |
| Setup from github config      | POST   | `/geonetwork/srv/api/aodn/setup`               | source=github | Edge, Staging |

### How the Setup works?

#### source=github (default)
Run the setup endpoint trigger geonetwork load the configuration file from github [config](./geonetwork-config/config.json),
this file is the blue-print to load other configuration from github that store under the geonetwork-config folder.

By default, it load from main branch, hence during your development, you may want to use the environment variable about to
force it load from different branch.

Given the configuration is store in main branch, that means changes to configuration require a pull request.

> The POST method can carry a body with the same format as the config.json file, if this appears, then the content
> in the body will be use instead of the config.json in github. Hence, you can run individual setup one by one

### Read Record Misc Info endpoint
Info not expose in regular api endpoints but needed for indexing, for example logo url of a dataset.

## Schema folder

The schema folder contains an open-api schema file from Genetwork4, you can get it from any instance
of GN4 (http://domain_name/geonetwork/srv/api/doc)

Once you have the json, you can generate code like the one here in Java to access GN4 via API.

## Use of S3

You can see a config file related to S3, however we do not use it because after experiment it, it
didn't support well as the GN4 will issue warning on file not found with relative folder name. The
code is just keep as a record.

## Utilities folder

You can use the available utilities inside the `utilities` folder of this repository. More details inside.
