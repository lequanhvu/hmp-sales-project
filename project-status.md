# Project Status

> Last updated: 2026-10-18 | By: @antigravity | Session: #3
>
> AI: update this file at the end of every session when asked.
> Follow this exact format. Preserve full milestone checklists and project specifications.

---

## Completed
- ✅ **Phase 1 — Identity, Roles & Permissions (Task 1 & Task 2 completed)**:
  - JPA Entities configured in `com.hmp.sales.user.entity`:
    - `Role` (bảng `roles`, khóa chính `role_id`, ràng buộc duy nhất trên `role_code`, cờ `is_deleted`, audit timestamps).
    - `Permission` (bảng `permissions`, khóa chính `permission_id`, `permission_name` duy nhất, ràng buộc `(api_path, method)`, phân hệ `module`, cờ `is_deleted`, audit timestamps).
    - `RolePermission` (bảng liên kết `role_permissions`, composite key `RolePermissionId`, hỗ trợ phân quyền động `PRODUCT_PRICE_VIEW`).
    - `User` (bảng `users`, khóa chính `user_id`, hỗ trợ `GOOGLE` và `LOCAL`, `ACTIVE`/`LOCKED`, ràng buộc duy nhất `email` và `(auth_provider, provider_subject)`).
  - Enums: `RoleCode` (`ADMIN`, `MANAGER`, `CUSTOMER`), `AuthProvider` (`GOOGLE`, `LOCAL`), `UserStatus` (`ACTIVE`, `LOCKED`).
  - Spring Data JPA Repositories in `com.hmp.sales.user.repository`:
    - `RoleRepository`: tìm theo `roleCode`, lọc `isDeleted = false`.
    - `PermissionRepository`: tìm theo `permissionName`, `apiPath` + `method`, lọc theo module.
    - `RolePermissionRepository`: tìm quyền theo role (`findPermissionsByRoleId`), kiểm tra quyền nhanh (`hasPermission`).
    - `UserRepository`: tìm theo `email`, `authProvider` + `providerSubject`, eager fetch role (`findByIdWithRole`, `findByEmailWithRole`).
  - Toàn bộ 35/35 unit và integration tests pass (thêm 12 repository test cases mới: `RoleRepositoryTest`, `PermissionRepositoryTest`, `RolePermissionRepositoryTest`, `UserRepositoryTest`).
- ✅ **Phase 0 — Foundation & Infrastructure completed**:
  - Spring Boot 4.1.1 project configured with Java 21, Spring Data JPA, Spring Security 7, OAuth2 Resource Server, Redis, Validation, Lombok, and H2 test DB.
  - Multi-profile YAML configuration (`application.yaml`, `application-dev.yaml`, `application-test.yaml`) with `AppProperties` for JWT, OTP, CORS, and order properties.
  - Standard API response wrappers: `ApiSuccessResponse<T>`, `ApiErrorResponse<T>`, `ValidationErrorResponse` matching `api-spec.md`.
  - Base custom exceptions: `ResourceNotFoundException`, `BusinessRuleException`, `UnauthorizedOperationException`, `InvalidOrderStateException`, `OtpVerificationException`.
  - Centralized `@RestControllerAdvice` (`GlobalExceptionHandler`) for Vietnamese standardized error responses and validation messages.
  - `RedisConfig` with `RedisTemplate<String, Object>` and `StringRedisTemplate`.
  - `SecurityConfig` with stateless OAuth2 Resource Server, HMAC-SHA256 `JwtDecoder`, custom `RestAuthenticationEntryPoint`, and `RestAccessDeniedHandler`.
  - `RedisTokenValidationFilter` enforcing active token verification against `auth:access:{jti}` in Redis.
  - 23 unit and integration tests passing (`ApiResponseTest`, `GlobalExceptionHandlerTest`, `RedisTokenValidationFilterTest`, `SalesBackendApplicationTests`).
- ✅ **Requirements baseline confirmed**:
  - Website purpose: direct online sales + product introduction/contact.
  - Initial scope: Vietnamese language, mobile responsive, individual customers, one province/city.
  - Guest browsing without authentication; login required for ordering.
  - Customer authentication via Google; forgot-password/change-password excluded for Google accounts.
  - OTP required when phone number is entered during ordering (saved on first entry, updateable later).
  - Business roles: Admin, Manager, Customer.
  - Product price visibility is permission-driven (`PRODUCT_PRICE_VIEW`), configurable by Admin.
  - Customer can see order total during confirmation step after Manager calculation.
  - Order cancellation reason is optional for every role; order status history tracked.
  - Category behaves as a self-referencing parent-child tree; Manager can create child category or product under selected category.
  - Product active/locking status supported (`ACTIVE`/`INACTIVE`).
  - Manufacturing focus for V1: Tôn sóng lợp mái (inputs: type, color, length/size, quantity).
  - Size-based manufacturing formula confirmed: `TotalPrice = UnitPrice × Size × Quantity`.
  - Storage roles: PostgreSQL is durable source of truth; Redis is used for active access tokens (`jti`) and OTP with TTL.
  - Refresh tokens stored as cryptographic hashes in PostgreSQL and rotated on every refresh.
  - **Out-of-scope for V1 confirmed**: Online payment, inventory management, promotions/coupons, product reviews, quotation management, laser cutting, warranty workflow, news/blog module.
- ✅ **Architecture documentation completed** (`architecture.md`):
  - High-level layered architecture (`Controller -> Service -> Repository`).
  - Guest, Customer, Manager, and Admin business flows.
  - Category tree navigation and management flow.
  - Permission-based price visibility flow.
  - Order calculation, confirmation, and cancellation flows.
  - Product active/inactive locking flow.
  - Redis OTP flow and notification dispatch flow.
- ✅ **Database schema design completed** (`database.md`):
  - PostgreSQL schema for 21 tables with explicit PK/FK relationships, constraints, and audit fields.
  - Identity/Security: `Role`, `User`, `Permission`, `RolePermission`, `RefreshToken`.
  - Catalog: `Category`, `Brand`, `Unit`, `Product`, `ProductImage`, `ProductVariant`, `Color`, `ProductColor`, `ProductPrice`.
  - Ordering: `CustomerAddress`, `Order`, `OrderItem`, `OrderStatusHistory`.
  - Platform: `Notification`, `SystemLog`, `SystemSetting`.
  - Redis key design: `auth:access:{jti}`, `otp:phone:<phone>`, and rate-limiting keys.
- ✅ **REST API specification completed** (`api-spec.md`):
  - Standard response format (`ApiSuccessResponse<T>`) and error envelope (`ApiErrorResponse<T>`).
  - Public catalog APIs, Google auth & token APIs, Customer profile/address/order APIs, Manager catalog/order processing APIs, Admin user/role/setting/audit APIs.
- ✅ **Phase 1 — Identity, Roles & Permissions completed**:
  - `Role`, `Permission`, `RolePermission`, `User` entities & JPA repositories with integration tests.
  - `DataInitializer` startup component: idempotent seed for default Roles, Permissions, and Role-Permission mappings.
  - `PermissionEvaluationService` (`permEval` bean) for dynamic SpEL permission checks from JWT authorities without DB round-trips.
  - `SystemLogService` stub logging audit operations.
  - `AdminUserService` & `AdminUserController`: user pagination/search, account locking/unlocking, soft-delete with token revocation, and Google-based Manager provisioning.
  - `AdminRoleService` & `AdminRoleController`: view role permissions, replace-all permission updates, and forced token revocation flow (Section 18).
  - Comprehensive unit & integration tests (55 passing tests).

## In Progress
_Nothing._

## Deferred Issues
- **[P2]** Google OAuth exchange architecture: finalize frontend callback vs backend authorization code verification contract.
- **[P2]** Production OTP & notification provider: select SMS / Zalo ZNS provider and configure API credentials.
- **[P2]** Exact application JWT issuer & asymmetric/symmetric signing key management strategy.
- **[P2]** Exact order status cancellation matrix: determine which statuses remain cancellable after Manager confirms order.
- **[P2]** Confirm whether all calculated orders require explicit Customer confirmation before moving to `PROCESSING`.
- **[P3]** Cloud/object storage provider (S3 / Cloudinary) for product images deferred to file module implementation.
- **[P3]** Concrete numeric validation constraints for sheet dimensions (length, thickness, min/max limits).
- **[P3]** Exact enumerated `Type` values for Tôn sóng variants and accessories (sườn, xối, nóc, máng).
- **[P3]** Production Redis reliability pattern (transactional outbox or Redis Stream consumer group with dead-letter retry).
- **[P3]** Add ERD visual diagram and Google OAuth sequence diagram to documentation.

## Warnings
- ⚠️ **Spring Boot 4 / Spring Security 7**: OAuth2 Resource Server validates JWT signature/claims, but application must also check active token existence in Redis via `jti` on every request.
- ⚠️ **PostgreSQL Source of Truth**: Redis is strictly for ephemeral data (OTP, active `jti`); never store durable business entities or refresh tokens in Redis.
- ⚠️ **Money Calculation (VND)**: All monetary values must use `BIGINT` or `INT` (never floating-point types). Manufacturing calculation formula: `TotalPrice = UnitPrice × Size × Quantity`.
- ⚠️ **Permission-Driven Price Visibility**: `PRODUCT_PRICE_VIEW` is a configurable permission controlled by Admin; never hard-code price visibility logic based on role.
- ⚠️ **Refresh Token Rotation**: Refresh tokens must be stored as cryptographic hashes in PostgreSQL; old tokens must be revoked atomically when issuing replacements, with token family reuse detection.
- ⚠️ **Data Integrity & Snapshots**: Historical order data (prices, addresses, receiver info) must remain immutable snapshots. Do not recalculate historical orders when current product prices change, and do not physically delete master data referenced by past orders.
- ⚠️ **Scope Protection**: Do NOT implement V1 excluded modules (payment gateways, promotions, reviews, inventory, quotation, warranty, laser cutting, blogs).

## Next Tasks
1. **[P1]** Implement Phase 2: Google OAuth token verification & user account resolution (`POST /api/v1/auth/google`).
2. **[P1]** Implement Phase 2: JWT token generation service (embed roles & permissions, register `auth:access:{jti}` in Redis).
3. **[P1]** Implement Phase 2: RefreshToken entity, repository & rotation mechanism with token family reuse detection.
4. **[P1]** Implement Phase 2: Redis phone OTP service with rate limiting and single-use verification.

## Milestones

### Phase 0 — Foundation & Infrastructure
- [x] Create Spring Boot 4.x project skeleton (Maven, application.yml, multi-profile config)
- [x] PostgreSQL datasource & JPA/Hibernate setup
- [x] Redis connection & RedisTemplate / Lettuce configuration
- [x] Base exception classes (`ResourceNotFoundException`, `BusinessRuleException`, `UnauthorizedOperationException`, `InvalidOrderStateException`, `OtpVerificationException`)
- [x] Centralized exception handling (`@RestControllerAdvice`, `GlobalExceptionHandler`)
- [x] Standard API response wrapper (`ApiSuccessResponse<T>`, `ApiErrorResponse<T>`)
- [x] Jakarta Bean Validation configuration
- [x] Spring Security 7 OAuth2 Resource Server baseline configuration
- [x] Redis-backed access token validation filter / bearer token interceptor (`jti`)

### Phase 1 — Identity, Roles & Permissions
- [x] Role entity & repository (`ADMIN`, `MANAGER`, `CUSTOMER`)
- [x] Permission entity & repository (including `PRODUCT_PRICE_VIEW`, `ORDER_*`, `PRODUCT_*`)
- [x] RolePermission join entity & repository
- [x] User entity & repository (supporting `GOOGLE` and `LOCAL` auth providers, `ACTIVE`/`LOCKED` status)
- [x] User unique constraints (`Email`, `(AuthProvider, ProviderSubject)`)
- [x] Role & Permission initial seed data / migration (`DataInitializer` idempotent startup runner)
- [x] Permission evaluation service / custom authorization manager for dynamic permission checks (`PermissionEvaluationService`)
- [x] Admin User management service & APIs (search, lock/unlock accounts, soft-delete, create Manager)
- [x] Admin Role & Permission management APIs (view/update role permissions with forced token revocation flow)

### Phase 2 — Authentication, Tokens & OTP
- [ ] Google OAuth token verification service & user account resolution (idempotent mapping)
- [ ] Application JWT token generation service (with unique `jti`, roles, permissions)
- [ ] Redis active access token registry (`auth:access:{jti}` with TTL)
- [ ] RefreshToken entity & repository with cryptographic hash storage, family tracking, and rotation
- [ ] Refresh token rotation logic with token reuse detection and revocation
- [ ] Auth endpoints:
  - [ ] `POST /api/v1/auth/google` (login / register with Google)
  - [ ] `POST /api/v1/auth/refresh` (rotate refresh token, issue new JWT)
  - [ ] `POST /api/v1/auth/logout` (revoke Redis access token and PostgreSQL refresh token)
- [ ] Redis phone OTP service:
  - [ ] 6-digit random OTP generation with TTL (`otp:phone:<phone>`)
  - [ ] Rate limiting for OTP generation and verification attempts
  - [ ] OTP verification and single-use invalidation
- [ ] Auth & OTP unit and integration tests

### Phase 3 — Catalog & Category Hierarchy
- [ ] Category entity (self-referencing parent-child tree) & repository
- [ ] Category tree validation (prevent circular hierarchy references)
- [ ] Brand entity & CRUD repository
- [ ] Unit entity & CRUD repository
- [ ] Product entity (`ACTIVE`/`INACTIVE` status, SKU, Brand, Category, Unit references)
- [ ] ProductImage entity (1 primary image constraint, max 10 secondary images)
- [ ] ProductVariant entity (spec, thickness, SKU, active state)
- [ ] Color & ProductColor entities & repositories
- [ ] ProductPrice entity & repository
- [ ] Product status management (active/locking logic: prevent ordering inactive products)
- [ ] Public catalog APIs:
  - [ ] `GET /api/v1/categories/tree` (hierarchical category tree)
  - [ ] `GET /api/v1/products` (filter, sort, search, conditional price visibility based on `PRODUCT_PRICE_VIEW`)
  - [ ] `GET /api/v1/products/{productId}` (detail, variants, images, conditional price visibility)
- [ ] Manager catalog management APIs:
  - [ ] Create child category or product under current category
  - [ ] Category CRUD & display order updates
  - [ ] Product CRUD, image management, variant management
  - [ ] Product lock/unlock endpoint
- [ ] Catalog unit and integration tests

### Phase 4 — Ordering & Manufacturing Price Calculation
- [ ] CustomerAddress entity, repository, and CRUD APIs (`GET/POST/PATCH/DELETE /api/v1/me/addresses`)
- [ ] Customer profile APIs (`GET /api/v1/me`, `PATCH /api/v1/me`)
- [ ] Order & OrderItem entities with immutable price snapshots (`UnitPrice`, `TotalPrice`, `TotalAmount`)
- [ ] Receiver snapshot fields on Order (`ReceiverName`, `ReceiverPhone`, address fields)
- [ ] OrderStatusHistory entity & repository
- [ ] Order creation flow:
  - [ ] Customer order submission with phone OTP verification
  - [ ] Validation for active products, valid variants, positive quantity and size
  - [ ] Initial state assignment: `PENDING`
  - [ ] Direct PostgreSQL order creation with atomic transaction (`PENDING` state)
- [ ] Manager order processing & calculation:
  - [ ] Order listing with filters, pagination, and detail view
  - [ ] Manager price calculation: `TotalPrice = UnitPrice × Size × Quantity`
  - [ ] State transition to `CONFIRMED`
- [ ] Customer order confirmation:
  - [ ] Customer views calculated order total
  - [ ] Customer confirms calculated order -> transition to `PROCESSING`
- [ ] Order cancellation flow:
  - [ ] Customer cancellation validation against allowed cancellation window
  - [ ] Manager cancellation according to business authorization
  - [ ] Optional cancellation reason for all roles
  - [ ] Immutable history logging in `OrderStatusHistory` for every valid transition
- [ ] Order calculation and lifecycle unit/integration tests

### Phase 5 — Platform, Audit & Notifications
- [ ] Notification entity & repository
- [ ] Notification service (dispatch in-app notifications on order status changes)
- [ ] Customer notification APIs (`GET /api/v1/me/notifications`, mark as read)
- [ ] External notification abstraction (email / Zalo ZNS provider stubs)
- [ ] SystemSetting entity, repository, and Admin configuration APIs
- [ ] SystemLog entity, repository, and business audit logging service
- [ ] Admin system audit log query APIs (`GET /api/v1/admin/logs`)

### Phase 6 — Comprehensive Testing & Hardening
- [ ] **JUnit 5 Unit Tests**:
  - [ ] Manufacturing price calculation formula (`UnitPrice × Size × Quantity`)
  - [ ] Invalid size, negative quantity, and inactive product order rejections
  - [ ] Dynamic permission evaluation (`PRODUCT_PRICE_VIEW`) for price visibility
  - [ ] Order status transition matrix validation
  - [ ] Customer cancellation window boundary tests
  - [ ] Optional cancellation reason validation
  - [ ] Category self-referencing cycle prevention
  - [ ] Google account idempotent mapping
  - [ ] OTP generation, TTL expiry, attempt rate limiting, and replay prevention
- [ ] **Integration Tests**:
  - [ ] Spring Security 7 OAuth2 Resource Server bearer token authentication
  - [ ] Redis active access token (`jti`) revocation & logout integration
  - [ ] Refresh token rotation, cryptographic hashing, and family reuse detection
  - [ ] PostgreSQL atomic transactions and rollback behavior
  - [ ] Redis OTP TTL and access token revocation validation
  - [ ] REST API controller request validation and error responses
- [ ] **End-to-End / Manual Verification Pass**:
  - [ ] Guest browse -> Google login -> order flow
  - [ ] Phone OTP challenge -> verify -> order submission
  - [ ] Manager receives order -> calculates price -> status update
  - [ ] Customer views calculated total -> confirms order
  - [ ] Customer cancellation within allowed window
  - [ ] Product lock prevents subsequent ordering
  - [ ] Admin enables `PRODUCT_PRICE_VIEW` for Customer -> prices become visible in product catalog

## Definition of Done

A feature / task is complete only when:

```text
Business rule documented
        +
Database/API updated if needed
        +
Implementation complete
        +
Authorization verified
        +
Validation + error handling added
        +
Relevant JUnit 5 tests pass
        +
Manual/integration verification complete where required
        =
DONE
```
