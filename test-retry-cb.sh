#!/bin/bash

# --- 2. RETRY TEST ---
echo "=== Testing Retry Logic ==="
echo "Sending a single request to the Failing Endpoint (/api/line/bakerloo/status)."
echo "Expected behavior: The request will hang for a moment while it attempts 3 backend retries, then eventually fail with HTTP 500."

# We'll use curl to fetch the HTTP status code
http_status=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/line/bakerloo/status)
echo "Response HTTP Status: $http_status"
echo "Check your application logs to see the Retry attempts!"
echo ""

# --- 3. CIRCUIT BREAKER TEST ---
echo "=== Testing Circuit Breaker ==="
echo "The Circuit Breaker is configured to open after 5 failed 5xx requests."
echo "We already sent 1 failure just now during the Retry Test."
echo "Sending 5 more rapid requests to the failing endpoint..."

for i in {1..5}
do
    status=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/line/bakerloo/status)
    echo "Request $i - HTTP Status: $status"
done

echo ""
echo "The Circuit Breaker should now be OPEN."
echo "Sending one final request. Expected behavior: Instant HTTP 503 Service Unavailable (Failing Fast)."

final_status=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/line/bakerloo/status)
echo "Response HTTP Status: $final_status"
