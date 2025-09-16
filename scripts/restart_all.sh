#!/bin/bash
set -e

echo "### Restarting all services with cache invalidation... ###"

# 1. 캐시 없이 모든 이미지를 다시 빌드
echo "--- Step 1: Building all images without cache ---"
docker compose build --no-cache

# 2. 기존 컨테이너를 중지 및 삭제하고 새로운 이미지로 컨테이너를 다시 생성 및 시작
echo "--- Step 2: Recreating all containers with new images ---"
docker compose up -d --force-recreate

echo "### All services have been updated. ###"
