#!/bin/sh

export UID=$(id -u)
export GID=$(id -g)

# If you see permission error on creating the cert in log, then there is a problem with the elasticdata,
# the elasticdata folder and it subdirectory needs to have $UID:$GID

# Create the folder if it doesn't exist
if [ ! -d "elasticdata" ]; then
    mkdir -p "elasticdata"
    chown $UID:$GID "elasticdata"

    mkdir -p "elasticdata/certs"
    chown $UID:$GID "elasticdata/certs"

    mkdir -p "elasticdata/elasticsearch-data"
    chown $UID:$GID "elasticdata/elasticsearch-data"

    mkdir -p "elasticdata/kibana-data"
    chown $UID:$GID "elasticdata/kibana-data"

    mkdir -p "elasticdata/enterprisesearch-data"
    chown $UID:$GID "elasticdata/enterprisesearch-data"
else
    chown -R $UID:$GID "elasticdata"
fi

docker-compose -f docker-es-compose.yml up

