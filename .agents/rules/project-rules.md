---
trigger: always_on
description: "Core implementation rules for the sales website using Spring Boot 4.x, Spring Security 7, JWT Resource Server, PostgreSQL, Redis, REST APIs, GitHub, and JUnit 5."
---

# Project Rules

> These rules apply to the entire backend project. Follow them before adding new dependencies, changing public API contracts, or implementing business logic.

## 1. Technology Baseline

The project uses:

| Area | Technology |
|---|---|
| IDE | Antigravity IDE |
| Backend | Spring Boot 4.x |
| Security | Spring Security 7 |
| Authentication | OAuth2 Resource Server + JWT Bearer Token |
| Database | Microsoft PostgreSQL |
| Temporary data / OTP / token state | Redis |
| API style | RESTful API |
| Testing | JUnit 5 |
| Source control | Git / GitHub |

Do not introduce a new framework or infrastructure component without a concrete project requirement.

---

## 2. General Coding Rules

- Use Java and Spring Boot conventions consistently.
- Prefer constructor injection.
- Keep classes focused on one responsibility.
- Keep business logic out of controllers.
- Prefer clear, explicit code over speculative abstractions.
- Avoid generic utility classes unless there is a demonstrated reuse case.
- Write meaningful Javadoc/comments for public classes, records, functions, DTOs, and service methods (see section 20).
- Do not hard-code pagination parameters (`page`, `size`) or limit constraints (e.g. maximum product images) in business logic. Always declare them in `application.yaml` under `app.pagination` (`default-page-size`, `max-page-size`) and `app.product` (`max-secondary-images`), bind through `AppProperties`, and inject/access them in services and business components. (In controllers, keep `@RequestParam(defaultValue = "...")` using numeric string literals like `"0"` and `"20"` to guarantee MockMvc standalone test compatibility).
- Do not silently change an established business rule.
- When a requirement is not defined, mark it as `TBD` rather than inventing behavior.

---

## 3. Package Structure

Use feature-oriented modules with clear technical layers. Cross-cutting packages (`common`, `config`, `security`) reside at the root, while all business domain modules reside inside the `feature` package:

```text
com.<company>.<project>
├── common
│   ├── exception
│   ├── response
│   ├── validation
│   └── util
├── config
├── security
└── feature
    ├── auth
    ├── category
    ├── notification
    ├── order
    ├── product
    ├── setting
    ├── systemlog
    └── user
```

A feature package under `feature/` may contain only the technical layers it actually needs:

```text
controller/
service/
repository/
entity/
dto/
mapper/
```

Do not create empty or unnecessary layers just to follow a template.

---

## 4. REST API Rules

- Use resource-oriented endpoint names.
- Use `/api/v1` as the default API version prefix unless the project contract already defines another path.
- Use HTTP methods according to REST semantics.
- Return DTOs instead of JPA entities.
- Validate incoming request DTOs with Jakarta Bean Validation.
- Use a consistent success/error response structure.
- Use appropriate HTTP status codes.
- Never expose stack traces, SQL errors, passwords, tokens, or internal implementation details to clients.
- Any API contract change must also be reflected in `api-spec.md`.

---

## 5. DTO and Mapping Rules

### Request DTO

Represents data accepted by an API endpoint.

### Response DTO

Represents data exposed to the client.

### Entity

Represents persistence and database state. Entities are not API contracts.

### Mapping

Prefer MapStruct when mapping becomes non-trivial and the project already uses it. Small, obvious mappings may remain explicit.

Never accidentally expose protected fields such as:

- password hashes
- provider identities
- internal permissions
- audit fields that are not part of the public contract
- product prices when the caller lacks `PRODUCT_PRICE_VIEW`

---

## 6. Security Rules

- Protected APIs require JWT authentication.
- Configure Spring Security 7 as an OAuth2 Resource Server for bearer JWTs.
- Access tokens are JWTs and must also be registered in Redis while active.
- The Resource Server must validate JWT signature/claims, and the application must additionally validate the token's Redis presence using its `jti`.
- Keep authentication and authorization separate.
- Authorization must be enforced on the backend; frontend hiding is not security.
- Use roles for coarse-grained access control.
- Use permissions for configurable actions.
- JWT Access Token only stores user roles claim (e.g. `["ROLE_ADMIN"]`, `["ROLE_CUSTOMER"]`), avoiding token bloat by omitting permissions.
- Permissions are cached in RAM via `PermissionAuthorizationManager` (Singleton) and matched dynamically using `AntPathMatcher` on `(apiPath, method)`.
- When Role permissions are updated in the database, `permissionAuthorizationManager.loadCache()` must be invoked immediately to invalidate and synchronize the RAM cache.
- Product price visibility MUST be controlled by the `PRODUCT_PRICE_VIEW` permission evaluated dynamically via `PermissionEvaluationService`.
- Do not hard-code a permanent rule such as `CUSTOMER cannot see price`.
- A locked account must not be allowed to use protected functionality according to the security policy.
- Refresh tokens are opaque values stored durably in PostgreSQL as hashes and are rotated on every successful refresh.
- A refresh request must invalidate/revoke the old refresh token before or atomically with creating its replacement.
- Never log JWTs, passwords, OTPs, refresh tokens, OAuth tokens, client secrets, or `Authorization` headers.

---

## 7. Google Authentication

Customer registration and login use Google.

Store the provider identity separately from the application's local user identifier:

```text
UserID
Provider
ProviderSubject
```

Rules:

- The same Google identity must resolve to the same Customer account.
- Do not silently create duplicate Customer accounts for the same provider identity.
- Do not add a local password-reset or change-password flow for Google-authenticated Customer accounts unless the requirement changes.
- Do not invent frontend callback behavior when it has not been specified.

---

## 8. Redis Rules

Redis is used for active access-token state and temporary OTP verification data. Orders and durable business records are managed directly in PostgreSQL.

### Access Tokens

The access JWT is stored in Redis while active:

```text
auth:access:{jti} -> <JWT>
TTL = access-token expiration
```

On every protected request:

```text
JWT signature/claims validation
        +
Redis auth:access:{jti} exists and is active
        =
Authenticated request
```

Refreshing or logging out removes/revokes the old access-token record in Redis.

Do not treat Redis as the durable source for refresh tokens.

### Refresh Tokens

Refresh tokens are opaque random values. Persist only a cryptographic hash in PostgreSQL.

Rotation pattern:

```text
R1 active in SQL
    |
    v
consume/revoke R1
    |
    +--> create R2
    +--> link R2 to same token family
    +--> issue A2
    +--> store A2 in Redis
```

A reused/revoked refresh token must be rejected. Whole-family revocation on reuse is subject to the final security policy.

### OTP

Store OTP data in Redis with TTL.

Example key:

```text
otp:phone:<phone-number>
```

Rules:

- An OTP is a sequence of six random digits.
- OTP must expire automatically.
- OTP must not be written to application logs.
- Rate-limit OTP requests and verification attempts with Redis.
- A successfully verified OTP must be consumed or invalidated. Once successfully verified, an OTP token cannot be accepted again for that same OTP challenge or session.

## 9. PostgreSQL and JPA Rules

- Use explicit primary-key and foreign-key relationships.
- Use `INT` or `BIGINT`for monetary values; never use floating-point types for money. Because the system will calculate in Vietnamese currency (VND)
- Use transactions for atomic multi-table business operations.
- Enforce important relational integrity with database constraints as well as Java validation where appropriate.
- Preserve historical order information through snapshots or immutable values.
- Do not physically delete records that are required by historical business data.

---

## 10. Category and Product Rules

### Category Tree

Categories are self-referencing and support parent-child relationships:

```text
Root Category
├── Child Category
│   ├── Child Category
│   └── Product
└── Product
```

The Manager can create either:

```text
[ Create Child Category ]
[ Create Product ]
```

under the currently selected category.

Prevent circular category relationships.

### Product Status

Minimum product states:

```text
ACTIVE
INACTIVE
```

Rules:

- `ACTIVE` products can be newly ordered.
- `INACTIVE` products cannot be newly ordered.
- Inactive products should not be shown in the normal Customer product listing.
- Do not physically delete a product if it is referenced by historical orders.

### Product Images

A product supports:

- 1 primary image
- up to 10 secondary images

### Product Variants

Product variants are supported, especially for manufacturing products.

Customer-entered manufacturing dimensions such as length belong to `OrderItem`, not to the static ProductVariant, unless a future requirement changes this decision.

---

## 11. Pricing Rules

Current confirmed manufacturing formula:

```text
TotalPrice = UnitPrice × Size × Quantity
```

Example:

```text
100,000 × 5 × 10 = 5,000,000
```

Rules:

1. The Manager triggers the calculation.
2. Store the applied `UnitPrice` on the order item as a snapshot.
3. Store the resulting `TotalPrice` on the order item.
4. Store the calculated `TotalAmount` on the Order.
5. The Customer may see the order total during the confirmation step.
6. Product price visibility on product pages is controlled by `PRODUCT_PRICE_VIEW`.
7. Do not expose a product price when the caller lacks that permission.
8. Do not assume a retail pricing formula unless the business requirement explicitly defines one.

---

## 12. Order Rules

The order lifecycle is:

```text
PENDING
   |
   v
CONFIRMED
   |
   v
PROCESSING
   |
   v
COMPLETED
```

Cancellation can occur according to the configured business rules:

```text
PENDING        ---> CANCELLED
CONFIRMED      ---> CANCELLED   (only if business rules allow it)
PROCESSING     ---> CANCELLED   (only if business rules allow it)
```

Every valid status change must create an `OrderStatusHistory` record.

Rules:

- Customer must be authenticated to create an order.
- An order may contain products from multiple categories.
- Customer cancellation is limited by the configured cancellation window.
- Manager cancellation is controlled by authorization/business rules.
- `CancellationReason` is optional for every role.
- Preserve receiver name, phone, and address snapshots on the Order.
- Never recalculate a historical order using the current Product price.

---

## 13. Service Layer Rules

- Services must strictly follow the `Interface + Impl` pattern (e.g., `OrderService` interface and `OrderServiceImpl` class).
- Controllers and other dependent classes must inject the Service interface, never the concrete implementation class.

Business rules belong in services or domain policies, especially:

- order status transitions
- cancellation-window validation
- product active/inactive checks
- database-driven permission checks
- price calculation
- OTP verification state
- category tree integrity

Controllers should handle HTTP concerns only:

```text
HTTP Request
    |
    v
Controller
    |
    v
Service
    |
    +--> Repository
    +--> Redis
    +--> Other Services
```

---

## 14. Exception Handling

Use centralized exception handling with `@RestControllerAdvice` or the project's equivalent.

Use meaningful business exceptions when the client needs a domain-level error, for example:

```text
ResourceNotFoundException
BusinessRuleException
UnauthorizedOperationException
InvalidOrderStateException
OtpVerificationException
```

Keep a final fallback handler for unexpected exceptions.

Do not wrap every service method in `catch (Exception)` merely to suppress or hide failures.

---

## 15. Testing Rules

JUnit 5 is required.

Every non-trivial business rule should have tests covering the normal path and important failure/edge cases.

Priority test areas:

- price calculation
- invalid quantity or size
- inactive product order attempt
- cancellation window
- cancellation with and without reason
- order status transition matrix
- permission-controlled price output
- Google account idempotency
- OTP expiry and OTP reuse prevention
- OTP rate limiting
- category cycle prevention
- locked-user authorization

Tests must not depend on production credentials or real external notification providers.

---

## 16. Logging and Audit Rules

- Keep technical application logs separate from business audit logs where appropriate.
- Never log secrets or authentication credentials.
- Important security, administrative, and business operations should create `SystemLog` records according to `architecture.md` and `database.md`.
- Log enough context to investigate a problem without exposing sensitive data.

---

## 17. Git and GitHub Rules

- Keep commits small and focused.
- Write meaningful commit messages.
- Do not commit secrets, environment-specific credentials, generated build output, or local IDE artifacts.
- Review tests before pushing changes.
- When an API or database contract changes, update the corresponding documentation in the same change.

---

## 18. Scope Control

Do not implement the following V1 exclusions unless an approved requirement change is recorded:

- online payment
- laser cutting steel sheet
- quotation management module
- promotions / coupons / quantity discounts
- product reviews
- blog/news/user-guide pages
- warranty workflow
- full inventory management
- Excel/PDF reporting and statistical reports

When a future feature is mentioned, document it as `Future` or `TBD` instead of implementing it prematurely.

---

## 19. Documentation Rules

The following project documents are the source of truth for implementation planning:

```text
AGENTS.md
architecture.md
database.md
api-spec.md
project-status.md
.agents/rules/project-rules.md
```

Before implementing a feature:

1. Check `architecture.md` for the business flow.
2. Check `database.md` for persistence requirements.
3. Check `api-spec.md` for API contracts.
4. Check `project-rules.md` for coding and security constraints.
5. Check `project-status.md` before marking work as completed.

When a requirement changes, update the documentation before generating large amounts of code.

---

## 20. Code Commenting Rules (Quy tắc comment code)

Mọi mã nguồn trong dự án phải tuân thủ các quy tắc comment và tài liệu hóa sau:

1. **Javadoc cho public components và API contracts**:
   - Mọi `class`, `record`, `interface`, DTO, response wrapper (`ApiSuccessResponse`, `ApiErrorResponse`), exception handler, và public service method đều phải có Javadoc mô tả rõ ràng trách nhiệm và mục đích nghiệp vụ.
2. **Đầy đủ các thẻ mô tả chi tiết**:
   - Sử dụng thẻ `@param` giải thích ý nghĩa từng tham số đầu vào.
   - Sử dụng `@param <T>` giải thích kiểu generic (nếu có).
   - Sử dụng `@return` mô tả giá trị và trạng thái của kết quả trả về.
   - Sử dụng `@throws` mô tả rõ các trường hợp ném ra ngoại lệ nghiệp vụ hoặc hệ thống.
3. **Tính nhất quán về ngôn ngữ**:
   - Viết comment súc tích, dễ hiểu, ưu tiên tiếng Việt chuẩn mực cho các thông điệp nghiệp vụ và tài liệu module; không dùng teencode hay từ ngữ tối nghĩa.
4. **Đồng bộ hóa khi cập nhật code**:
   - Bất kỳ khi nào logic, tham số, mã lỗi hay hành vi của hàm thay đổi, lập tức cập nhật comment đi kèm. Không để comment lỗi thời (outdated) gây hiểu lầm.
5. **Tránh comment thừa thãi**:
   - Không viết comment lặp lại tên hàm một cách sáo rỗng (ví dụ: `// get name` cho `getName()`). Chỉ giải thích mục đích, logic đặc thù, hoặc bối cảnh nghiệp vụ.
6. **Bảo mật và an toàn dữ liệu**:
   - Tuyệt đối không đưa mật khẩu, JWT token, refresh token, secret key, mã OTP, payload nhạy cảm hoặc thông tin định danh cá nhân vào comment code.

