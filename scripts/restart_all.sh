#!/bin/bash
echo "### Restarting all services based on docker-compose.yml... ###"

# --build 옵션으로 이미지 변경이 필요한 경우 새로 빌드하고,
# 그렇지 않은 서비스는 설정만 반영하여 다시 시작합니다.
docker-compose up -d --build

echo "### All services have been updated. ###"
