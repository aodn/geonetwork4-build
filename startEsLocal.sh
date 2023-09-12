#!/bin/sh

export UID=$(id -u)
export GID=$(id -g)

# If you see permission error on creating the cert in log, then there is a problem with the ip where
# the program have no permission to access it.

# Create the folder if it doesn't exist
if [ ! -d "elasticdata" ]; then
    mkdir -p "elasticdata"
    chown $UID:$GID "elasticdata"

    mkdir -p "elasticdata/elasticsearch-data"
    chown $UID:$GID "elasticdata/elasticsearch-data"

    mkdir -p "elasticdata/kibana-data"
    chown $UID:$GID "elasticdata/kibana-data"

    mkdir -p "elasticdata/enterprisesearch-data"
    chown $UID:$GID "elasticdata/enterprisesearch-data"
fi

docker-compose -f docker-es-compose.yml up

