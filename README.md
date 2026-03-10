## Overview

This document outlines the technical design of a payments API that sits between a merchant and an acquiring bank. It validates payment requests, forwards them to the bank, stores the outcome, and lets merchants look up past payments.

## Quick Start

```bash
./gradlew bootRun    # starts on port 9090
./gradlew test       # runs the test suite
```

The gateway expects the bank simulator to be running at `http://localhost:8080`.

| Method | Path | What it does |
| --- | --- | --- |
| POST | `/api/payments` | Submit a payment for processing |
| GET | `/api/payments/{id}` | Look up a previously processed payment |

---

## Sample Requests

### Process a payment (Authorized)

Card numbers ending in an odd digit are authorized by the bank.

```bash
curl -s -X POST http://localhost:9090/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "cardNumber": "2222405343248877",
    "expiryMonth": 4,
    "expiryYear": 2030,
    "currency": "GBP",
    "amount": 100,
    "cvv": "123"
  }'
```

Response (`200 OK`):

```json
{
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "status": "Authorized",
  "lastFourCardDigits": "8877",
  "expiryMonth": 4,
  "expiryYear": 2030,
  "currency": "GBP",
  "amount": 100
}
```

### Process a payment (Declined)

Card numbers ending in an even digit are declined by the bank.

```bash
curl -s -X POST http://localhost:9090/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "cardNumber": "2222405343248878",
    "expiryMonth": 4,
    "expiryYear": 2030,
    "currency": "USD",
    "amount": 5000,
    "cvv": "456"
  }'
```

Response (`200 OK`):

```json
{
  "id": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
  "status": "Declined",
  "lastFourCardDigits": "8878",
  "expiryMonth": 4,
  "expiryYear": 2030,
  "currency": "USD",
  "amount": 5000
}
```

### Process a payment (Rejected → validation failure)

Invalid input is rejected before reaching the bank.

```bash
curl -s -X POST http://localhost:9090/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "cardNumber": "123",
    "expiryMonth": 13,
    "expiryYear": 2020,
    "currency": "XYZ",
    "amount": -1,
    "cvv": "AB"
  }'
```

Response (`400 Bad Request`):

```json
{
  "status": "Rejected",
  "errors": [
    "Card number must be between 14 and 19 characters",
    "Expiry month must be between 1 and 12",
    "Card expiry date must be in the future",
    "Currency must be one of: USD, GBP, EUR",
    "Amount must be a positive integer",
    "CVV must contain only numeric characters",
    "CVV must be 3 or 4 characters"
  ]
}
```

### Retrieve a payment

```bash
curl -s http://localhost:9090/api/payments/a1b2c3d4-e5f6-7890-abcd-ef1234567890
```

Response (`200 OK`):

```json
{
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "status": "Authorized",
  "lastFourCardDigits": "8877",
  "expiryMonth": 4,
  "expiryYear": 2030,
  "currency": "GBP",
  "amount": 100
}
```

### Retrieve a non-existent payment

```bash
curl -s http://localhost:9090/api/payments/00000000-0000-0000-0000-000000000000
```

Response (`404 Not Found`):

```json
{
  "error": "Payment not found with id: 00000000-0000-0000-0000-000000000000"
}
```

---

## Design Considerations

### Architecture

The payment service follows a clean Controller → Service → Client/Repository pattern:

- **Controller** handles HTTP concerns (request binding, validation trigger, response codes).
- **Service** contains business logic (mapping between DTOs, determining payment status, orchestrating persistence).
- **BankClient** encapsulates external communication with the acquiring bank.
- **Repository** abstracts storage behind an interface.

### Protecting card data

Full card numbers and CVVs are **never persisted**. Only the last four digits of the card number are stored and returned in responses. The full card number is used transiently (solely to build the bank request) and then discarded. This minimises the blast radius of a data breach and aligns with PCI-DSS principles around limiting cardholder data storage.

### Amount Represented as Integer (Minor Currency Units)

The `amount` field is an integer representing the smallest unit of the currency (e.g., pence for GBP, cents for USD/EUR). This avoids floating-point precision issues that arise with monetary calculations. For example, GBP 10.99 is represented as `1099`.

### Three-Way Payment Status Model

Payments can be in one of three states:

- **Authorized →**  payment was approved by the bank.
- **Declined →** payment was rejected by the bank (insufficient funds, stolen card, etc.).
- **Rejected** →  The gateway rejected the request before it reached the bank (validation failure).

This distinction lets merchants differentiate between *their own* request errors (`Rejected`) and bank-side decisions (`Authorized` / `Declined`).

### Declarative Validation with Custom Constraints

Input validation uses Jakarta Bean Validation with three custom constraint annotations:

- **`@FutureExpiry`** → A class-level constraint that validates the combined expiry month/year is in the future. This is class-level because the check requires both fields together.
- **`@NumericOnly`** → Ensures card number and CVV contain only digit characters.
- **`@SupportedCurrency`** → Restricts the currency to the supported set (USD, GBP, EUR).

Keeping validation declarative on the DTO keeps the service layer free of validation logic, and invalid requests are rejected before any bank communication occurs.

### DTO Separation (Merchant API vs Bank API)

The merchant-facing DTOs (`PaymentRequest` / `PaymentResponse`) are structurally different from the bank-facing DTOs (`BankRequest` / `BankResponse`). The bank expects a combined `expiry_date` string (`"MM/YYYY"`) and uses `snake_case` field names, while the merchant API uses separate `expiryMonth` / `expiryYear` fields and `camelCase`. Mapping between these in the service layer means either contract can evolve independently without affecting the other.

### Repository Abstraction

`PaymentRepository` is defined as an interface with `save` and `findById` methods. The current implementation (`InMemoryPaymentRepository`) uses a `ConcurrentHashMap` for thread-safe in-memory storage. Because the service depends only on the interface, swapping to a database-backed implementation (e.g., JPA) requires no changes to the service or controller layers.

### Bank Client Error Handling

The `BankClient` translates low-level HTTP and connectivity exceptions into a domain-specific `BankUnavailableException`. This prevents Spring’s `RestClient` exceptions from leaking into the service layer and allows the global exception handler to return a meaningful `502 Bad Gateway` response to the merchant.

### One place for all error handling

The `GlobalExceptionHandler` maps every expected failure to a predictable response shape:

- Validation errors → `400` with `{ status: "Rejected", errors: [...] }`
- Malformed JSON → `400` with `{ status: "Rejected", errors: ["Malformed request body"] }`
- Payment not found → `404` with `{ error: "..." }`
- Bank unreachable → `502` with `{ error: "..." }`

Merchants can rely on a consistent structure regardless of what went wrong.

## Assumptions

1. **In-memory storage is fine for this exercise.** Data doesn’t survive a restart. In production, this would be backed by a proper database.
2. **No auth on the API.** There’s no API key or token check. In a real system, you would need merchant authentication, as this is a payment API.
3. **No idempotency keys.** Every POST creates a new payment. If a merchant retries due to a network error, they’ll get a duplicate charge. A production gateway would accept an idempotency key to handle this safely.
4. **Amounts are in minor units.** The caller sends `100` for one dollar/pound/euro, not `1.00`. This is stated in the API contract and is consistent with how most payment platforms work.
5. **Only USD, GBP, and EUR are supported.** These are hard-coded. Adding more currencies would mean a code change in production; I would pull this from config or a database.
6. **Card numbers are 14–19 digits.** This covers Visa, Mastercard, Amex, and most other networks per ISO/IEC 7812.
7. **CVV is 3 or 4 digits.** Three for most cards, four for Amex.
8. **Expiry must be strictly in the future.** The system treats the current month as expired. Some gateways are more lenient and accept it until the last day of the month, but we went with the stricter interpretation.
9. **The bank call is synchronous.** We send the request and wait. No timeouts or retries; those would be important in production.
10. **No currency conversion.** The gateway passes the amount and currency straight to the bank. No FX logic.

---

## Testing Strategy

The test suite is structured in three layers:

- **Validation tests** (`PaymentRequestValidationTest`) → Unit tests that verify each constraint annotation in isolation using a programmatic `Validator`, without starting the Spring context.
- **Service tests** (`PaymentServiceTest`) → Unit tests with Mockito that verify business logic (status mapping, card masking, persistence calls) in isolation from the HTTP layer and the bank.
- **Integration tests** (`PaymentControllerIntegrationTest`) → Full Spring Boot tests with `MockMvc` that exercise the complete request pipeline (serialisation, validation, service, exception handling) with only the `BankClient` mocked.

---

## Project Structure

```
src/main/java/com/checkout/gateway/
├── GatewayApplication.java
├── client/
│   └── BankClient.java              # talks to the acquiring bank
├── controller/
│   └── PaymentController.java       # REST endpoints
├── dto/
│   ├── BankRequest.java             # what we send to the bank
│   ├── BankResponse.java            # what the bank sends back
│   ├── PaymentRequest.java          # what the merchant sends us
│   └── PaymentResponse.java         # what we send the merchant
├── exception/
│   ├── BankUnavailableException.java
│   ├── GlobalExceptionHandler.java  # centralised error responses
│   └── PaymentNotFoundException.java
├── model/
│   ├── Payment.java                 # domain entity
│   └── PaymentStatus.java           # AUTHORIZED, DECLINED, REJECTED
├── repository/
│   ├── InMemoryPaymentRepository.java
│   └── PaymentRepository.java       # storage interface
└── validation/
    ├── FutureExpiry.java
    ├── FutureExpiryValidator.java
    ├── NumericOnly.java
    ├── NumericOnlyValidator.java
    ├── SupportedCurrency.java
    └── SupportedCurrencyValidator.java
```

---

## Areas for Improvement

- **Idempotency keys** → Accept a merchant-provided idempotency key to safely handle retries.
- **Merchant authentication** → Secure the API with API keys or OAuth2 tokens.
- **Persistent storage** → Replace the in-memory store with a relational database (e.g., PostgreSQL).
- **Resilience patterns** → Add timeouts, retries, and circuit breakers to the bank client.
- **Structured logging** → Add correlation IDs for request tracing across the gateway and bank.
- **Containerization** → Provide a `Dockerfile` and `docker-compose.yml` to run the gateway alongside the
bank simulator.
- **API documentation** → Generate OpenAPI/Swagger specs from the controller annotations.
- **Luhn check** → Validate card numbers using the Luhn algorithm as an additional pre-flight check.