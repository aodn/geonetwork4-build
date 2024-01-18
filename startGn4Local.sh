#!/bin/sh

# Start a local instance for development only, we should use Elastic.co for other env.

export UID=$(id -u)
export GID=$(id -g)

if [ -d "gn4_data" ]; then
  # Create the folder if it doesn't exist
  mkdir -p "gn4_data"
  chown $UID:$GID "gn4_data"
else
  chown -R $UID:$GID "gn4_data"
fi

# If you run in EC2, you need to change the docker-es-compose.yml localhost in cert to the hostname of EC2 and install docker
# docker-compose and sudo chmod 666 /run/docker.sock
docker-compose -f docker-gn-compose.yml down -v || true
docker-compose -f docker-gn-compose.yml up --build

