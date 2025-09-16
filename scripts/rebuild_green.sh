#!/bin/bash
SERVICES_TO_REBUILD=$@

if [ -z "$SERVICES_TO_REBUILD" ]; then
    echo "No specific services to rebuild."
    exit 0
fi

echo "### Rebuilding specific services: $SERVICES_TO_REBUILD ###"
docker compose up -d --no-deps --build $SERVICES_TO_REBUILD