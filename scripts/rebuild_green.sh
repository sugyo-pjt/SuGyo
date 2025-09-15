#!/bin/bash
echo "### Rebuilding and restarting green environment... ###"

docker-compose up -d --no-deps --build spring-app-green

echo "### Green environment has been rebuilt and restarted. ###"