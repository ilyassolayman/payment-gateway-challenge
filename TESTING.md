# Manual Testing Guide

All commands assume the application is running on `http://localhost:8090`.

---

## 1. Happy path — Authorized payment

Card ending in `7` (odd) → bank returns Authorized.

```bash
curl -s -X POST http://localhost:8090/api/v1/payment -H "Content-Type: application/json" -d '{"card_number":"2222405343248877","expiry_month":4,"expiry_year":2027,"currency":"GBP","amount":100,"cvv":"123"}'
```

**Expected response (201 Created):**
```json
{
  "id": "<uuid>",
  "status": "Authorized",
  "cardNumberLastFour": "8877",
  "expiryMonth": 4,
  "expiryYear": 2027,
  "currency": "GBP",
  "amount": 100
}
```

---

## 2. Retrieve the authorized payment

Copy the `id` from the response above and substitute it below.

```bash
curl -s http://localhost:8090/api/v1/payment/<id>
```

**Expected response (200 OK):**
```json
{
  "id": "<uuid>",
  "status": "Authorized",
  "cardNumberLastFour": "8877",
  "expiryMonth": 4,
  "expiryYear": 2027,
  "currency": "GBP",
  "amount": 100
}
```

---

## 3. Bank response — Declined payment (card ending in even number)

The bank simulator declines payments where the card number ends in an even digit. This exercises the `DECLINED` path through `BankPaymentService`.

```bash
curl -s -X POST http://localhost:8090/api/v1/payment -H "Content-Type: application/json" -d '{"card_number":"2222405343248112","expiry_month":4,"expiry_year":2027,"currency":"GBP","amount":100,"cvv":"123"}'
```

**Expected response (201 Created):**
```json
{
  "id": "<uuid>",
  "status": "Declined",
  "cardNumberLastFour": "8112",
  "expiryMonth": 4,
  "expiryYear": 2027,
  "currency": "GBP",
  "amount": 100
}
```

---

## 4. Bank response — Bank unavailable (card ending in 0)

The bank simulator returns a `503 Service Unavailable` when the card ends in `0`. This triggers `BankPaymentRequestException` which maps to a `502 Bad Gateway` — distinguishing a bank-side failure from a validation failure or a successful decline.

```bash
curl -s -X POST http://localhost:8090/api/v1/payment -H "Content-Type: application/json" -d '{"card_number":"2222405343248870","expiry_month":4,"expiry_year":2027,"currency":"GBP","amount":100,"cvv":"123"}'
```

**Expected response (502 Bad Gateway):**
```json
{
  "errorMessage": "Bank payment service is unavailable",
  "timestamp": "<timestamp>"
}
```

---

## 6. Unhappy path — Single validation error (card number too long)

Card number has 20 digits, exceeding the 19-digit maximum.

```bash
curl -s -X POST http://localhost:8090/api/v1/payment -H "Content-Type: application/json" -d '{"card_number":"22224053432488770000","expiry_month":4,"expiry_year":2027,"currency":"GBP","amount":100,"cvv":"123"}'
```

**Expected response (400 Bad Request):**
```json
{
  "status": "Rejected",
  "rejectionReasons": [
    "Card number must be between 14 and 19 digits"
  ]
}
```

---

## 7. Unhappy path — Multiple validation errors (expired card, card number too long, CVV too long)

```bash
curl -s -X POST http://localhost:8090/api/v1/payment -H "Content-Type: application/json" -d '{"card_number":"22224053432488770000","expiry_month":1,"expiry_year":2020,"currency":"GBP","amount":100,"cvv":"12345"}'
```

**Expected response (400 Bad Request):**
```json
{
  "status": "Rejected",
  "rejectionReasons": [
    "Card number must be between 14 and 19 digits",
    "Card has expired",
    "CVV must be 3 or 4 characters"
  ]
}
```

---

## 8. Endpoint which doesn't exist

```bash
curl -s http://localhost:8090/api/v1/unknown/endpoint
```

**Expected response (404 Not Found):**
```json
{
  "errorMessage": "No endpoint found for http://localhost:8090/api/v1/unknown/endpoint",
  "timestamp": "<timestamp>"
}
```

---

## 9. Payment ID that doesn't exist

```bash
curl -s http://localhost:8090/api/v1/payment/00000000-0000-0000-0000-000000000000
```

**Expected response (404 Not Found):**
```json
{
  "errorMessage": "Payment not found for ID: 00000000-0000-0000-0000-000000000000",
  "timestamp": "<timestamp>"
}
```
