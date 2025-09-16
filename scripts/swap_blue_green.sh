#!/bin/bash
set -e # 오류 발생 시 즉시 스크립트 중단

NGINX_CONF_PATH="./Nginx/conf.d/default.conf"

echo "### Swapping Blue and Green environments... ###"

API_TARGET=$(grep 'location /api/' -A 1 $NGINX_CONF_PATH | grep 'proxy_pass' | awk -F'//' '{print $2}' | sed 's/;//' | tr -d '[:space:]\r')
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

# API 경로의 프록시를 기존 TEST 타겟으로 변경
sed -i "s|proxy_pass http://$API_TARGET;|proxy_pass http://$TEST_TARGET;|" $TEMP_CONF
# TEST 경로의 프록시를 기존 API 타겟으로 변경
sed -i "s|proxy_pass http://$TEST_TARGET;|proxy_pass http://$API_TARGET;|" $TEMP_CONF

mv $TEMP_CONF $NGINX_CONF_PATH

echo "Swap logic applied. New configuration:"
cat $NGINX_CONF_PATH

docker compose exec nginx nginx -s reload

echo "### Swap complete! ###"
echo "New API Target (Blue) is now: $TEST_TARGET"
echo "New Test Target (Green) is now: $API_TARGET"
