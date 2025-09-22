#!/bin/bash
set -e # 오류 발생 시 즉시 스크립트 중단

NGINX_CONF_PATH="./Nginx/nginx.conf"

echo "### Swapping Blue and Green environments... ###"

API_TARGET=$(grep 'location / {' -A 1 $NGINX_CONF_PATH | grep 'proxy_pass' | awk -F'//' '{print $2}' | sed 's/;//' | tr -d '[:space:]\r')
TEST_TARGET=$(grep 'location /test/' -A 1 $NGINX_CONF_PATH | grep 'proxy_pass' | awk -F'//' '{print $2}' | sed 's/;//' | tr -d '[:space:]\r')

echo "Current API Target (Blue): $API_TARGET"
echo "Current Test Target (Green): $TEST_TARGET"

# 두 타겟이 동일하면 오류를 발생시키고 중단 (안전장치)
if [ "$API_TARGET" = "$TEST_TARGET" ]; then
    echo "Error: API and Test targets are the same. Aborting swap."
    exit 1
fi

# API 타겟 -> 새로운 테스트 타겟
# TEST 타겟 -> 새로운 API 타겟
TEMP_CONF=$(mktemp)
cp $NGINX_CONF_PATH $TEMP_CONF

# 1. / 경로의 프록시를 임시 토큰으로 변경
sed -i "/location \/ {/,/}/ s|proxy_pass http://$API_TARGET;|proxy_pass http://__TEMP_TARGET__;|g" $TEMP_CONF
# 2. /test/ 경로의 프록시를 기존 API 타겟으로 변경
sed -i "/location \/test\//,/}/ s|proxy_pass http://$TEST_TARGET;|proxy_pass http://$API_TARGET;|g" $TEMP_CONF
# 3. 임시 토큰을 기존 TEST 타겟으로 최종 변경
sed -i "s|proxy_pass http://__TEMP_TARGET__;|proxy_pass http://$TEST_TARGET;|g" $TEMP_CONF

mv $TEMP_CONF $NGINX_CONF_PATH

echo "Swap logic applied. New configuration:"
cat $NGINX_CONF_PATH

docker exec nginx nginx -s reload

echo "### Swap complete! ###"
echo "New API Target (Blue) is now: $TEST_TARGET"
echo "New Test Target (Green) is now: $API_TARGET"
