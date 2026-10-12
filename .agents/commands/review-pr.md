Review the code changes in this PR. Check every item below — flag violations clearly.

## 1. Architecture & Layering
- [ ] Controller only receives request → calls service → returns response (no business logic in controllers)
- [ ] **Interface + Impl pattern strictly enforced**: Service uses Interface (`XxxService`) + Implementation (`XxxServiceImpl`)
- [ ] Controller injects the Service interface, NEVER the implementation class (`XxxServiceImpl`)
- [ ] Service implementation handles business rules and domain logic
- [ ] No HTTP concerns in service/repository layers (no `HttpServletRequest`, `ResponseEntity`, `HttpStatus`)
- [ ] Entity ↔ DTO conversion happens in service layer or dedicated mappers, not in controllers
- [ ] Clear layered separation: `Controller -> Service (Interface) -> ServiceImpl -> Repository`

## 2. Type Safety, DTOs & Price Visibility
- [ ] Request / Response use DTOs / Java Records (Entities are NEVER returned directly from REST controllers)
- [ ] Request DTOs have Jakarta Validation annotations (`@NotNull`, `@NotBlank`, `@Size`, `@Min`, etc.)
- [ ] Response DTOs do NOT expose sensitive fields (passwords, provider identities, internal tokens)
- [ ] **Dynamic Price Visibility**: Response DTO does NOT expose product price when caller lacks `PRODUCT_PRICE_VIEW` permission
- [ ] Price visibility is driven by permission evaluation, NOT hard-coded by role (e.g., `Customer` can see prices if Admin grants permission)

## 3. Dependency Injection & Code Quality
- [ ] Constructor injection only — NO `@Autowired` on fields
- [ ] Injected dependencies are `private final`
- [ ] Injected service dependencies are interface types (e.g., `private final UserService userService`), not concrete Impl classes
- [ ] Method length: `< 50 lines` (complex logic extracted into private helpers)
- [ ] File length: Controllers/DTOs/Repos `< 200 lines`; Services/Entities/Config target `< 300 lines` (soft limit `< 400 lines`); Test files target `< 500 lines`
- [ ] No `@Data` on JPA entities (use `@Getter`, `@Setter`, explicit `equals`/`hashCode`)
- [ ] Logger uses `{}` placeholders, never string concatenation
- [ ] No sensitive data in logs (passwords, OTPs, JWTs, refresh tokens, client secrets)

## 4. Error Handling & Exceptions
- [ ] Custom business exceptions used (not generic `RuntimeException` or suppressed `catch (Exception)`)
- [ ] Exception naming matches domain rules: `ResourceNotFoundException`, `BusinessRuleException`, `UnauthorizedOperationException`, `InvalidOrderStateException`, `OtpVerificationException`
- [ ] Controller does NOT have try/catch — handled centrally via `@RestControllerAdvice` / `GlobalExceptionHandler`
- [ ] Proper HTTP status codes returned (400, 401, 403, 404, 409, 422, 500)

## 5. Security & Authentication
- [ ] Spring Security 7 OAuth2 Resource Server bearer token authentication configured
- [ ] Protected endpoints enforce server-side authentication and role/permission checks
- [ ] Access token validity verified against active Redis registry (`auth:access:{jti}`)
- [ ] Google authentication maps idempotently by `(AuthProvider, ProviderSubject)` without duplicate accounts
- [ ] Refresh tokens stored ONLY in PostgreSQL as cryptographic hashes (NEVER plaintext, NEVER in Redis)
- [ ] Refresh token rotation revokes old token atomically and detects token family reuse
- [ ] Account lock status (`Status == 'LOCKED'`) correctly blocks protected actions

## 6. PostgreSQL Persistence & Data Integrity
- [ ] PostgreSQL is the authoritative persistence store for all durable business data
- [ ] `FetchType.LAZY` on all `@OneToMany`, `@ManyToOne`, `@ManyToMany` relationships
- [ ] `@Enumerated(EnumType.STRING)` for all enums — never `ORDINAL`
- [ ] `@Transactional` used on multi-step write operations that must be atomic
- [ ] No N+1 query issues (use `join fetch`, entity graphs, or batch fetching where appropriate)
- [ ] **Monetary Values (VND)**: Stored as `BIGINT` or `INT` — NEVER floating-point types (`Double`, `Float`)
- [ ] **Order Immutability**: Historical order snapshots (`UnitPrice`, `TotalPrice`, `TotalAmount`, receiver info, delivery address) are immutable and never recalculated from current Product prices
- [ ] **No Physical Deletion**: Master data (Category, Product, User) referenced by historical orders is never hard deleted (use soft delete or active/inactive flags)

## 7. Redis & Ephemeral State Boundary
- [ ] Redis is used strictly for ephemeral state, TTL data, and rate limiting (NO order queue in Redis)
- [ ] Durable entities (Orders, Products, Users, Refresh Tokens) are NOT stored in Redis
- [ ] Active access tokens stored with expiration TTL: `auth:access:{jti}`
- [ ] OTP keys use proper format and TTL: `otp:phone:<phone>`
- [ ] OTP rate limiting and attempt limits enforced via Redis
- [ ] Verified OTP is invalidated immediately (single-use, prevents replay attacks)
- [ ] Orders are managed directly in PostgreSQL; Redis is NOT used for order queues

## 8. Business Rules & Scope Protection (V1)
- [ ] **Manufacturing Calculation Formula**: `TotalPrice = UnitPrice × Size × Quantity`
- [ ] **Order Lifecycle**: `PENDING -> CONFIRMED -> PROCESSING -> COMPLETED` with transitions logged to `OrderStatusHistory`
- [ ] **Order Cancellation**: Cancellation reason is optional (`cancellationReason` can be null); Customer cancellation respects configured time window
- [ ] **Product Status**: Inactive products (`INACTIVE`) cannot be newly ordered
- [ ] **Category Tree**: Category parent-child relationship prevents circular hierarchy cycles
- [ ] **Product Images**: Exactly 1 primary image (`isPrimary = true`), maximum 10 secondary images (total max 11)
- [ ] **Scope Protection**: PR does NOT implement V1 out-of-scope features (online payment gateways, inventory tracking, coupon/promotions, product reviews, quotation management, laser cutting, warranty module, blog/news)

## 9. Testing (JUnit 5)
- [ ] PR includes relevant JUnit 5 tests covering happy paths, edge cases, and failure scenarios
- [ ] Unit tests cover business calculations (e.g., price calculation, size validation, cancellation window)
- [ ] Authorization and permission-based price visibility tests present
- [ ] Tests do NOT depend on live external providers (SMS, Zalo, production Google credentials)
- [ ] All new and existing tests pass cleanly

## 10. Documentation & Status Sync
- [ ] If business flow changed: `architecture.md` updated
- [ ] If schema or database constraints changed: `database.md` updated
- [ ] If API endpoints, request/response models changed: `api-spec.md` updated
- [ ] If coding or architectural rules affected: `.agents/rules/project-rules.md` updated
- [ ] Progress tracked and updated in `project-status.md`

## Summary
After checking, provide:
1. **Blockers** — must fix before merge (violates security, data integrity, or core business rules)
2. **Suggestions** — recommended improvements (code style, performance, test edge cases)
3. **Good parts** — what was implemented well and adheres to project standards