#!/usr/bin/env bash
set -uo pipefail

# Embassy Flow — Demo MVP DoD smoke check.
#
# App-in lokalde işlədiyini və seed data-nın mövcud olduğunu yoxlayır.
# Bütün müştəri + portal axınını canlı DB üzərində işlədir:
#   24 endpoint + variantlar.
#
# Demo dəyərlər:
#   FİN 5D7X9Q2 → customerId 1 (Aydan Əhadova)
#   OTP (demo)  -> 123456
#   Portal      -> admin@italy / demo1234
#
# Usage: scripts/dod-check.sh [BASE_URL]

BASE_URL="${1:-http://localhost:8080}"
FIN="5D7X9Q2"
OTP="123456"
CUSTOMER_ID="1"
EMBASSY_ID="1"
ACCOUNT_ID="1"
CARD_ID="1"

pass=0
fail=0

request() {
    local label="$1"
    local expect="$2"
    shift 2
    local body
    if ! body=$(curl -sS --fail-with-body "$@"); then
        printf "FAIL  %-42s (http error)\n" "$label"
        fail=$((fail + 1))
        return 1
    fi
    if echo "$body" | grep -qi "$expect"; then
        printf "PASS  %-42s\n" "$label"
        pass=$((pass + 1))
        echo "$body"
    else
        printf "FAIL  %-42s (expected '%s')\n%s\n" "$label" "$expect" "$body"
        fail=$((fail + 1))
        return 1
    fi
}

json_value() {
    echo "$1" | jq -r "$2"
}

echo "== Müştəri axını =="

request "GET /document-types #1" 'ACCOUNT_STATEMENT' \
    "$BASE_URL/api/v1/document-types"

request "GET /embassies #3" 'İtaliya' \
    "$BASE_URL/api/v1/embassies"

resp=$(curl -sS --fail-with-body -X POST "$BASE_URL/api/v1/auth/fin/verify" \
    -H 'Content-Type: application/json' \
    -d "{\"fin\":\"$FIN\"}")
echo "$resp" | grep -qi 'Aydan' && { printf "PASS  %-42s\n" "POST /auth/fin/verify #5"; pass=$((pass + 1)); } \
    || { printf "FAIL  %-42s\n%s\n" "POST /auth/fin/verify #5" "$resp"; fail=$((fail + 1)); }

request "POST /auth/otp/send #6" 'demoOtp' \
    -X POST "$BASE_URL/api/v1/auth/otp/send" \
    -H 'Content-Type: application/json' \
    -d "{\"customerId\":$CUSTOMER_ID}"

resp=$(curl -sS --fail-with-body -X POST "$BASE_URL/api/v1/auth/otp/validate" \
    -H 'Content-Type: application/json' \
    -d "{\"customerId\":$CUSTOMER_ID,\"otp\":\"$OTP\"}")
CUSTOMER_TOKEN=$(json_value "$resp" '.accessToken')
echo "$resp" | grep -qi 'demo-token-customer' && { printf "PASS  %-42s\n" "POST /auth/otp/validate #7"; pass=$((pass + 1)); } \
    || { printf "FAIL  %-42s\n%s\n" "POST /auth/otp/validate #7" "$resp"; fail=$((fail + 1)); }

resp=$(curl -sS --fail-with-body -X POST "$BASE_URL/api/v1/orders" \
    -H 'Content-Type: application/json' \
    -H "Authorization: Bearer $CUSTOMER_TOKEN" \
    -d '{"documentType":"ACCOUNT_STATEMENT","language":"AZ"}')
ORDER_ID=$(json_value "$resp" '.orderId')
echo "$resp" | grep -qi '"orderId"' && { printf "PASS  %-42s\n" "POST /orders #2 (draft yarat)"; pass=$((pass + 1)); } \
    || { printf "FAIL  %-42s\n%s\n" "POST /orders #2 (draft yarat)" "$resp"; fail=$((fail + 1)); }

request "PUT /orders/{id} #9 (səfirlik)" 'CREATED' \
    -X PUT "$BASE_URL/api/v1/orders/$ORDER_ID" \
    -H 'Content-Type: application/json' \
    -H "Authorization: Bearer $CUSTOMER_TOKEN" \
    -d "{\"embassyId\":$EMBASSY_ID,\"language\":\"AZ\"}"

request "PUT /orders/{id}/identity #10" 'OTP_VERIFIED' \
    -X PUT "$BASE_URL/api/v1/orders/$ORDER_ID/identity" \
    -H 'Content-Type: application/json' \
    -H "Authorization: Bearer $CUSTOMER_TOKEN" \
    -d "{\"customerId\":$CUSTOMER_ID}"

request "POST /orders/{id}/items #11" '6M' \
    -X POST "$BASE_URL/api/v1/orders/$ORDER_ID/items" \
    -H 'Content-Type: application/json' \
    -H "Authorization: Bearer $CUSTOMER_TOKEN" \
    -d "{\"items\":[{\"accountId\":$ACCOUNT_ID,\"language\":\"AZ\",\"period\":\"6M\",\"statementType\":\"ALL\",\"equivalentCurrency\":false}]}"

resp=$(curl -sS --fail-with-body -X POST "$BASE_URL/api/v1/orders/$ORDER_ID/preview" \
    -H "Authorization: Bearer $CUSTOMER_TOKEN")
DOC_NO=$(json_value "$resp" '.documentNumber')
VERIFY_CODE=$(json_value "$resp" '.verificationCode')
echo "$resp" | grep -qi 'verificationCode' && { printf "PASS  %-42s\n" "POST /orders/{id}/preview #13"; pass=$((pass + 1)); } \
    || { printf "FAIL  %-42s\n%s\n" "POST /orders/{id}/preview #13" "$resp"; fail=$((fail + 1)); }

resp=$(curl -sS --fail-with-body -X POST "$BASE_URL/api/v1/orders/$ORDER_ID/pay" \
    -H 'Content-Type: application/json' \
    -H "Authorization: Bearer $CUSTOMER_TOKEN" \
    -d "{\"cardId\":$CARD_ID,\"cvv\":\"000\"}")
PAY_TXN=$(json_value "$resp" '.transactionNo')
echo "$resp" | grep -qi 'SUCCESS' && { printf "PASS  %-42s\n" "POST /orders/{id}/pay #14"; pass=$((pass + 1)); } \
    || { printf "FAIL  %-42s\n%s\n" "POST /orders/{id}/pay #14" "$resp"; fail=$((fail + 1)); }

request "GET /orders?customerId=1 #12" "AR-2026" \
    "$BASE_URL/api/v1/orders?customerId=$CUSTOMER_ID&status=ALL" \
    -H "Authorization: Bearer $CUSTOMER_TOKEN"

request "GET /orders/{id} #4 (status)" 'PAYMENT_RECEIVED' \
    "$BASE_URL/api/v1/orders/$ORDER_ID" \
    -H "Authorization: Bearer $CUSTOMER_TOKEN"

request "GET /customers/1/accounts #8" 'accountNumber' \
    "$BASE_URL/api/v1/customers/$CUSTOMER_ID/accounts" \
    -H "Authorization: Bearer $CUSTOMER_TOKEN"

request "GET /payments #22 (tarixçə)" "$PAY_TXN" \
    "$BASE_URL/api/v1/payments?customerId=$CUSTOMER_ID&page=0&size=10" \
    -H "Authorization: Bearer $CUSTOMER_TOKEN"

request "GET /payments #22b (status filtri)" "$PAY_TXN" \
    "$BASE_URL/api/v1/payments?customerId=$CUSTOMER_ID&status=SUCCESS" \
    -H "Authorization: Bearer $CUSTOMER_TOKEN"

request "GET /notifications #23" 'unreadCount' \
    "$BASE_URL/api/v1/notifications?customerId=$CUSTOMER_ID&page=0&size=10" \
    -H "Authorization: Bearer $CUSTOMER_TOKEN"

NTF_ID=$(curl -sS --fail-with-body "$BASE_URL/api/v1/notifications?customerId=$CUSTOMER_ID&page=0&size=1" \
    -H "Authorization: Bearer $CUSTOMER_TOKEN" | jq -r '.notifications[0].notificationId')

request "PATCH /notifications/{id}/read #24" '"read":true' \
    -X PATCH "$BASE_URL/api/v1/notifications/$NTF_ID/read" \
    -H "Authorization: Bearer $CUSTOMER_TOKEN"

request "GET /docs/{id}/verify #20 (public QR)" 'true' \
    "$BASE_URL/api/v1/portal/documents/$DOC_NO/verify?code=$VERIFY_CODE"

echo
echo "== Səfirlik portalı =="

resp=$(curl -sS --fail-with-body -X POST "$BASE_URL/api/v1/portal/auth/login" \
    -H 'Content-Type: application/json' \
    -d '{"username":"admin@italy","password":"demo1234"}')
PORTAL_TOKEN=$(json_value "$resp" '.accessToken')
echo "$resp" | grep -qi 'demo-portal-token' && { printf "PASS  %-42s\n" "POST /portal/auth/login #15"; pass=$((pass + 1)); } \
    || { printf "FAIL  %-42s\n%s\n" "POST /portal/auth/login #15" "$resp"; fail=$((fail + 1)); }

request "GET /portal/stats #16" '"total"' \
    "$BASE_URL/api/v1/portal/stats" \
    -H "Authorization: Bearer $PORTAL_TOKEN"

request "GET /portal/documents #17" "$DOC_NO" \
    "$BASE_URL/api/v1/portal/documents" \
    -H "Authorization: Bearer $PORTAL_TOKEN"

request "GET /portal/documents/{id} #18" "$DOC_NO" \
    "$BASE_URL/api/v1/portal/documents/$DOC_NO" \
    -H "Authorization: Bearer $PORTAL_TOKEN"

request "GET /portal/documents/{id}/download #21" 'AB' \
    "$BASE_URL/api/v1/portal/documents/$DOC_NO/download" \
    -H "Authorization: Bearer $PORTAL_TOKEN"

request "PUT /portal/documents/{id}/status #19" 'COMPLETED' \
    -X PUT "$BASE_URL/api/v1/portal/documents/$DOC_NO/status" \
    -H 'Content-Type: application/json' \
    -H "Authorization: Bearer $PORTAL_TOKEN" \
    -d '{"status":"COMPLETED"}'

echo
echo "Nəticə: $pass passed, $fail failed"
[ "$fail" -eq 0 ] || exit 1