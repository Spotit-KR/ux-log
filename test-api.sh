#!/bin/bash

BASE_URL="http://localhost:8080"
ADMIN_USER="${ADMIN_USERNAME:-admin}"
ADMIN_PASS="${ADMIN_PASSWORD:-admin}"

echo "=== UX-Log API Test ==="
echo ""

# Project 1 (test1) - ID: 1
echo "--- Project 1 (test1) ---"

echo "1. Track page view (thread, post #1)"
curl -s "$BASE_URL/api/track?projectId=1&channel=thread&postNumber=1"
echo " [OK]"

echo "2. Track page view (thread, post #2)"
curl -s "$BASE_URL/api/track?projectId=1&channel=thread&postNumber=2"
echo " [OK]"

echo "3. Track page view (instagram)"
curl -s "$BASE_URL/api/track?projectId=1&channel=instagram"
echo " [OK]"

echo "4. Submit email (thread)"
curl -s -X POST "$BASE_URL/api/email" \
  -H "Content-Type: application/json" \
  -d '{"projectId": 1, "email": "user1@example.com", "channel": "thread", "postNumber": "1"}'
echo ""

echo "5. Submit email (instagram)"
curl -s -X POST "$BASE_URL/api/email" \
  -H "Content-Type: application/json" \
  -d '{"projectId": 1, "email": "user2@example.com", "channel": "instagram"}'
echo ""

echo ""

# Project 2 (test2) - ID: 2
echo "--- Project 2 (test2) ---"

echo "1. Track page view (twitter)"
curl -s "$BASE_URL/api/track?projectId=2&channel=twitter&postNumber=100"
echo " [OK]"

echo "2. Track page view (twitter)"
curl -s "$BASE_URL/api/track?projectId=2&channel=twitter&postNumber=101"
echo " [OK]"

echo "3. Submit email (twitter)"
curl -s -X POST "$BASE_URL/api/email" \
  -H "Content-Type: application/json" \
  -d '{"projectId": 2, "email": "user3@example.com", "channel": "twitter", "postNumber": "100"}'
echo ""

echo ""

# Check stats (requires authentication)
echo "=== Check Stats (with auth) ==="

echo "Project 1 stats:"
curl -s -u "$ADMIN_USER:$ADMIN_PASS" "$BASE_URL/api/admin/projects/1/stats" | python3 -m json.tool 2>/dev/null || curl -s -u "$ADMIN_USER:$ADMIN_PASS" "$BASE_URL/api/admin/projects/1/stats"
echo ""

echo "Project 2 stats:"
curl -s -u "$ADMIN_USER:$ADMIN_PASS" "$BASE_URL/api/admin/projects/2/stats" | python3 -m json.tool 2>/dev/null || curl -s -u "$ADMIN_USER:$ADMIN_PASS" "$BASE_URL/api/admin/projects/2/stats"
echo ""

echo "=== Done ==="
echo "Visit http://localhost:8080/admin to see the dashboard"
