# AGENTS.md — Sales Website Workspace Instructions

## 1. Project identity

This repository is the backend for a real-world Vietnamese sales website.

### Primary stack:
- Java / Spring Boot 4.x
- Spring Security 7
- JWT authentication with OAuth2 Resource Server
- RESTful API
- PostgreSQL
- Redis
- JUnit 5
- Git / GitHub
- Antigravity IDE

### Read these documents before making non-trivial changes:
- `architecture.md` — system architecture and backend business flows
- `database.md` — database model and data integrity rules
- `api-spec.md` — REST API contract
- `.agents/rules/project-rules.md` — coding and implementation rules
- `project-status.md` — implementation checklist and current progress

## 2. Source of truth

Do not invent business behavior when the requirements are silent. Mark unresolved points as `TBD` and keep the implementation isolated so the decision can be changed later.

### Current confirmed decisions include:
- Website is Vietnamese and mobile responsive.
- Initial customers are individual customers in one province/city.
- Guests can browse; login is required to place orders.
- Customer authentication is via Google.
- OTP is required when a customer enters a phone number. Upon successful OTP verification, the phone number is directly saved to the user's account in PostgreSQL, and an active verification proof is maintained in Redis.
- Admin, Manager, Customer are the business roles.
- Admin controls permissions, including whether Customer can see product prices.
- Product price visibility must be permission-driven, not hard-coded to a role.
- Customer can see the order total when the calculated order is presented for confirmation.
- Cancellation reason is optional for every role.
- Order status history must be stored.
- Categories are a parent-child tree. A Manager can create a child category or a product under the current category, similar to a file-system folder flow.
- Version 1 does not require online payment, inventory management, promotions, reviews, quotation management, laser cutting, warranty management, or a public news/blog module.
- Current manufacturing product flow is focused on `Tôn sóng lợp mái` and related accessories like `sườn, xối, nóc, máng`.
- Redis is used for temporary/ephemeral data (active access tokens) and OTP verification; PostgreSQL remains the source of truth for business data and direct order processing.

## 3. Agent behavior

### Before changing code:
1. Identify the affected business flow.
2. Check the related architecture/database/API rule.
3. Preserve existing contracts unless the task explicitly changes them.
4. Prefer small, focused changes over broad rewrites.
5. Add or update tests for business rules and security-sensitive behavior.

### When implementing a feature:
- Write clear Javadoc comments for all public classes, records, functions, DTOs, and service methods.
- Update the relevant `.md` document when the behavior or contract changes.
- Update `project-status.md` only after the implementation state actually changes.
- Do not mark work complete based only on generated code; verify it with tests or a documented manual check.

## 4. Backend architecture expectations

Use a layered backend with clear separation of concerns:
`Controller -> Service -> Repository`
with cross-cutting components for security, validation, exception handling, Redis, notifications, and logging.

Controllers must remain thin. Business rules belong in services/domain-oriented components. Repositories handle persistence only.

Use DTOs at API boundaries. Do not expose JPA entities directly from REST controllers.

Use transactions for multi-step PostgreSQL business operations that must be atomic.

Use Redis only where its characteristics are useful: TTL data, short-lived state, rate limiting, cache, and access token state. Do not make Redis the authoritative store for orders, products, users, refresh tokens or order history. Orders are persisted and processed directly in PostgreSQL.

## 5. Security

All protected REST endpoints must be secured by Spring Security 7.

Use JWT bearer authentication through OAuth2 Resource Server. Access tokens are JWTs and are also registered in Redis for server-side token-state validation/revocation.
Authorization must be enforced server-side; never rely on the client hiding buttons or URLs.

Separate authentication from authorization:
- Authentication: establish who the caller is.
- Authorization: check roles/permissions for the requested action.

Price visibility is a permission, not a hard-coded Customer/Manager/Admin branch.

Refresh tokens must be stored in PostgreSQL as hashes, never plaintext. Refresh-token rotation must revoke the old token before/while issuing the replacement.

Never log access tokens, passwords, OTP values, OAuth credentials, or other secrets.

## 6. API and data rules

All API contracts must be documented in `api-spec.md`.

Validate request data at the API boundary and enforce critical integrity constraints in PostgreSQL as well.

Use stable IDs and explicit foreign keys. Preserve historical order snapshots even if a customer changes their address or a product is later locked.

Do not physically delete business data that is referenced by historical orders unless a carefully designed archival policy exists.

## 7. Testing

JUnit 5 is the required test framework.

Prioritize tests for:
- order calculation
- order status transitions and cancellation rules
- permission-based price visibility
- Google authentication/account creation behavior
- OTP expiry and verification logic
- category parent-child constraints
- product active/inactive ordering rules
- controller/service authorization

Use unit tests for isolated business logic and integration tests where Spring Security, persistence, Redis, or transaction behavior is part of the contract.

## 8. Out-of-scope protection

Do not add V1 functionality merely because it is common in e-commerce. In particular, do not implement online payment, promotions/coupons, product reviews, inventory, quotation management, laser cutting, or warranty modules unless the requirements are explicitly changed.
