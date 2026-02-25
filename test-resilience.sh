#!/bin/bash

# Define colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

APP_URL="http://localhost:8080"

echo -e "${YELLOW}Starting Resilience Testing Script...${NC}\n"

# --- 1. Retry Logic Test ---
echo -e "${GREEN}=== 1. Testing Retry Logic ===${NC}"
echo "Sending a single request to the Failing Endpoint (/api/line/bakerloo/status)."
echo "Expected behavior: The request will hang for a moment while it attempts 3 backend retries, then eventually fail with HTTP 500."

start_time=$(date +%s)
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -H "Accept-Version: v1" ${APP_URL}/api/line/bakerloo/status)
end_time=$(date +%s)
echo "Time elapsed: $((end_time - start_time)) seconds. Response HTTP Status: $STATUS"
echo "Check your application logs to see the Retry attempts!"
echo ""
sleep 2

# --- 2. Circuit Breaker Test ---
echo -e "${GREEN}=== 2. Testing Circuit Breaker ===${NC}"
echo "The Circuit Breaker is configured to open after 5 failed 5xx requests."
echo "We already sent 1 failure just now during the Retry Test."
echo "Sending 5 more rapid requests to the failing endpoint..."

for i in {1..5}; do
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" -H "Accept-Version: v1" ${APP_URL}/api/line/bakerloo/status)
  echo "Request $i - HTTP Status: $STATUS"
done

echo ""
echo "The Circuit Breaker should now be OPEN."
echo "Sending one final request. Expected behavior: Instant HTTP 503 Service Unavailable (Failing Fast)."

start_time=$(date +%s)
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -H "Accept-Version: v1" ${APP_URL}/api/line/bakerloo/status)
end_time=$(date +%s)
echo "Time elapsed: $((end_time - start_time)) seconds. Response HTTP Status: $STATUS"
echo ""
sleep 2

# --- 3. Rate Limiting Test ---
echo -e "${GREEN}=== 3. Testing Rate Limiting ===${NC}"
echo "Sending 105 rapid requests to the Success Endpoint (/api/line/victoria/status)."
echo "Expected behavior: First 100 succeed, last 5 fail with HTTP 429 Too Many Requests."

SUCCESS_COUNT=0
RATELIMIT_COUNT=0

for i in {1..105}; do
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" -H "Accept-Version: v1" ${APP_URL}/api/line/victoria/status)
  if [ "$STATUS" -eq 200 ]; then
    SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
  elif [ "$STATUS" -eq 429 ]; then
    RATELIMIT_COUNT=$((RATELIMIT_COUNT + 1))
  fi
done

echo "Requests resulting in 200 OK: $SUCCESS_COUNT"
echo "Requests resulting in 429 Too Many Requests: $RATELIMIT_COUNT"

echo -e "\n${YELLOW}Resilience Testing Script Complete.${NC}"
