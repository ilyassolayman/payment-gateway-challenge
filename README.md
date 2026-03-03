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

A missing card number and a card number that is too short are both cases of a merchant supplying invalid information. They should produce the same response shape — a `PostPaymentResponse` with `status: REJECTED` and a list of rejection reasons.

### Trade-off
By not using Bean Validation, we lose the convenience of annotation-driven validation and must maintain `PaymentRequestValidator` manually. This is an accepted trade-off in favour of a consistent API contract — all validation failures, whether structural or business-rule, return a `REJECTED` response with actionable rejection reasons.

The only annotations used on `PostPaymentRequest` are `@JsonProperty` for correct JSON field name mapping, which are deserialization hints rather than validation.

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
Controller → Service → Repository, with clear separation of concerns. Bank communication is isolated in its own `IBankPaymentService`.

**Validation before bank call**
Invalid requests are rejected immediately without hitting the bank simulator, keeping `REJECTED` status distinct from `DECLINED` (which comes from the bank).

**In-memory storage**
`PaymentsRepository` uses a `ConcurrentHashMap` — intentional for this challenge, no persistence across restarts.

**Null-safe serialization**
`@JsonInclude(NON_NULL)` on `PostPaymentResponse` ensures rejected payments only return `status` and `rejectionReasons`, not a payload full of nulls.

**Centralised error handling**
All exceptions flow through `CommonExceptionHandler` — no try/catch scattered across controllers or services.

**Versioned API**
`/api/v1/payment` prefix allows future API versions without breaking existing clients.

---

## Assumptions

- **Currency support** is intentionally limited to a small set of ISO codes (not all 180+)
- **Card number** is stored as last 4 digits only — full number is never persisted, only forwarded to the bank
- **Expiry validation** checks combined month+year is in the future, not just the year
- **Amount** is an integer (minor currency units, e.g. pence/cents) — no decimal support
- **Bank simulator** is always available locally; no retry logic is implemented
- **No authentication** — the API is open, assumed to be handled at a gateway/infrastructure level outside this service
- **One payment per request** — no batch processing
