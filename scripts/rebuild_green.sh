#!/bin/bash
set -e

SERVICES_TO_REBUILD=$@

if [ -z "$SERVICES_TO_REBUILD" ]; then
    echo "No specific services to rebuild."
    exit 0
fi

echo "### Rebuilding specific services: $SERVICES_TO_REBUILD ###"

# 1. 지정된 서비스만 다시 빌드
echo "--- Step 1: Building specified services ---"
docker compose build $SERVICES_TO_REBUILD

# 2. 지정된 서비스와 그에 의존하는 서비스들을 다시 생성 및 시작
echo "--- Step 2: Recreating specified containers ---"
docker compose up -d --force-recreate --no-deps $SERVICES_TO_REBUILD

echo "### Specified services have been updated. ###"
