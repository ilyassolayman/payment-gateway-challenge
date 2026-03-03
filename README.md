# Payment Gateway

For sample curl commands covering happy path, unhappy path, and error scenarios, see [TESTING.md](TESTING.md).

---

# Design Decisions

## Static utility classes vs Spring-managed beans

`PaymentMapper` and `PaymentRequestValidator` are implemented as static utility classes rather than Spring `@Component` beans.

### Decision
Both classes are stateless pure utilities with no injected dependencies and no realistic alternate implementations. Converting them to static utility classes with a private constructor was a deliberate choice to favour simplicity over flexibility.

### Why static is acceptable here
- Neither class has any state or injected dependencies
- There is no realistic scenario where a different implementation would be swapped in
- The ability to mock them in tests has no practical value — their behaviour is simple and deterministic
- `PaymentService` still has a compile-time dependency on both classes, but this is an accepted trade-off for reduced complexity in the context of this assessment

---

## Validation strategy — Bean Validation vs PaymentRequestValidator

### Decision
Bean Validation annotations (`@NotNull`, `@Size`, `@Pattern` etc.) are intentionally not used on `PostPaymentRequest`. All validation is handled by `PaymentRequestValidator`.

### Reasoning
There are two categories of validation:
- **Structural** — is the field present? (e.g. missing card number)
- **Business rule** — is the value valid? (e.g. card number too short, unsupported currency)

Adding Bean Validation annotations would cause Spring to throw `MethodArgumentNotValidException` before the request reaches the service, returning a plain `400 Bad Request` with no `REJECTED` status. This breaks the product requirement:

> *"Rejected — No payment could be created as invalid information was supplied to the payment gateway"*

A missing card number and a card number that is too short are both cases of a merchant supplying invalid information. They should produce the same response shape — a `PaymentResponse` with `status: REJECTED` and a list of rejection reasons.

### Trade-off
By not using Bean Validation, we lose the convenience of annotation-driven validation and must maintain `PaymentRequestValidator` manually. This is an accepted trade-off in favour of a consistent API contract — all validation failures, whether structural or business-rule, return a `REJECTED` response with actionable rejection reasons.

The only annotations used on `PaymentRequest` are `@JsonProperty` for correct JSON field name mapping, which are deserialization hints rather than validation.

---

## Card expiry validation

### Assumption
A card is considered valid through the last day of the month printed on the card. A card showing `05/2026` remains valid until May 31, 2026 and expires on June 1, 2026.

### Implementation
The expiry check uses `expiry.isBefore(YearMonth.now())` rather than `!expiry.isAfter(YearMonth.now())`. This means:
- A card expiring in the **current month** is accepted
- A card expiring in a **previous month** is rejected

### Source
This reflects standard card network behaviour (Visa, Mastercard) where the card remains usable until the end of the printed expiry month.

---

## Key design considerations

**Layered architecture**
Controller → Service → Repository, with clear separation of concerns. Bank communication is isolated in its own `IBankPaymentService`. The repository stores a `Payment` domain entity, not an API DTO — the mapper converts between them at the service boundary. This keeps the domain model independent of the API contract and gives domain fields (e.g. `authorizationCode`) a natural home that doesn't bleed into the response shape.

**Validation before bank call**
Invalid requests are rejected immediately without hitting the bank simulator, keeping `REJECTED` status distinct from `DECLINED` (which comes from the bank).

**Rejected payments are not stored**
When a payment is rejected due to validation failure, no record is written to the repository and no ID is assigned. This is a deliberate decision aligned with the spec — *"no payment could be created as invalid information was supplied"*. A rejected payment was never created as a payment entity, so there is nothing to store or retrieve.

In a production system, rejected attempts would be captured by a separate audit or event log (e.g., a message bus or observability platform) rather than the payment repository. That concern belongs to infrastructure, not the gateway's domain model. Mixing rejected attempts into the payment store would blur the distinction between a payment record and an access log.

**In-memory storage**
`PaymentsRepository` uses a `ConcurrentHashMap` — intentional for this challenge, no persistence across restarts.

**Single `PaymentResponse` for both POST and GET**
Both endpoints return `PaymentResponse`. A separate `GetPaymentResponse` and `PostPaymentResponse` were considered but not introduced — the response contracts are identical, and splitting them into two classes with identical fields would be duplication without benefit. If the contracts diverged (e.g. POST surfacing `authorizationCode` to the merchant, GET omitting it), that would be the signal to split them.

**Null-safe serialization**
`@JsonInclude(NON_NULL)` on `PaymentResponse` ensures rejected payments only return `status` and `rejectionReasons`, not a payload full of nulls.

**Centralised error handling**
All exceptions flow through `CommonExceptionHandler` — no try/catch scattered across controllers or services.

**Versioned API**
`/api/v1/payment` prefix allows future API versions without breaking existing clients.

---

## Card number normalisation

Whitespace is stripped from the card number at the start of `PaymentService.processPayment()` before validation or mapping runs. This allows naturally formatted card numbers (e.g. `"2222 4053 4324 8877"`) to be accepted without rejecting valid input due to spacing.

### Trade-off
This is a single line on the request model's setter call in the service, so no dedicated normaliser class was introduced. A `PaymentRequestNormaliser` would be the right abstraction if normalisation grew to cover multiple fields with non-trivial rules — but for one field and one operation, that abstraction would be premature.

An alternative considered was a custom Jackson `@JsonDeserialize` deserializer on the `cardNumber` field of `PaymentRequest`. This would strip whitespace at deserialization time, keeping the service clean. However, it requires an additional deserializer class, making it more complex than the one-liner it replaces. The service approach was preferred for simplicity.

---

## Test strategy

Three levels of tests are used:

**Unit tests — `PaymentRequestValidatorTest`, `PaymentMapperTest`**
Pure logic with no Spring context. Each validation rule and each mapping is tested in isolation. Fast and exhaustive — every error code has a corresponding test.

**Service unit tests — `PaymentServiceTest`**
`PaymentService` is tested with Mockito mocks for `IPaymentsRepository` and `IBankPaymentService`. This verifies the orchestration logic — validation gating, repository interactions, and exception propagation — without hitting any I/O.

**Integration tests — `PaymentGatewayControllerIntegrationTest`**
Full Spring context with `MockMvc` and `MockRestServiceServer` standing in for the bank simulator. These tests exercise the full HTTP stack — routing, serialization, status codes, response bodies, and the `Location` header — against realistic request/response payloads.

---

## Bank exception hierarchy

Three distinct exceptions represent different upstream failure modes, all mapped to `502 Bad Gateway` in `CommonExceptionHandler`:

- **`BankPaymentClientException`** — the bank returned a `4xx`. This means the gateway sent a malformed request. It is a gateway bug, not a bank outage.
- **`BankPaymentRequestException`** — the bank returned a `5xx` or was unreachable. The bank is unavailable.
- **`BankPaymentResponseException`** — the bank returned `200` but with a null body. The response was structurally invalid.

All three map to `502 Bad Gateway` rather than `500 Internal Server Error`. `502` communicates that the gateway received an invalid or absent response from an upstream dependency — which is semantically correct in all three cases. `500` would imply the gateway itself crashed unexpectedly.

---

## Assumptions

- **Currency support** is intentionally limited to a small set of ISO codes (not all 180+)
- **Card number** is stored as last 4 digits only — full number is never persisted, only forwarded to the bank
- **Expiry validation** checks combined month+year is in the future, not just the year
- **Amount** is an integer (minor currency units, e.g. pence/cents) — no decimal support
- **Bank simulator** is always available locally; no retry logic is implemented
- **No authentication** — the API is open, assumed to be handled at a gateway/infrastructure level outside this service
- **One payment per request** — no batch processing
- **No idempotency key** — submitting the same payment twice creates two records with different UUIDs. In production, an idempotency key (merchant-generated, passed as a request header) would prevent duplicate charges on retry. This is a known production concern out of scope for this assessment.
- **Authorization code stored but not exposed** — the bank simulator returns an `authorization_code` alongside the `authorized` flag. This is stored on the `Payment` domain object but intentionally not surfaced in the API response, as it is not part of the specified response contract. In production it would be returned to the merchant and used for dispute management, reconciliation, and downstream settlement flows.
- **`authorized` flag not stored separately** — the bank's `authorized` boolean is used to derive `PaymentStatus` (AUTHORIZED/DECLINED) and is not stored redundantly alongside it. The status field is the canonical representation of the bank's decision within this system.
