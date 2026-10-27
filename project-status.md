# Project Status

> Last updated: 2026-10-27 | By: @antigravity | Session: #6
>
> AI: update this file at the end of every session when asked.
> Follow this exact format. Preserve full milestone checklists and project specifications.

---

## Completed
- ✅ **Phase 3 — Catalog & Category Hierarchy completed**:
  - `Category` entity & `CategoryRepository`: Cấu trúc cây tự tham chiếu cha - con (`parent_category_id`), phát hiện và chặn quan hệ vòng (cycle detection), hỗ trợ mô hình quản lý giống hệ thống thư mục (tạo danh mục con hoặc sản phẩm trực tiếp dưới danh mục hiện tại).
  - Master data động do Admin quản trị (`Brand`, `Unit`, `Color`):
    - Đầy đủ Entity, Repository, Service (`BrandService`, `UnitService`, `ColorService`), Controller (`/api/v1/admin/brands/**`, `/api/v1/admin/units/**`, `/api/v1/admin/colors/**`).
    - Hỗ trợ CRUD, phân trang, tìm kiếm, khóa/mở khóa (`ACTIVE`/`LOCKED`), kiểm tra ràng buộc toàn vẹn chặn xóa (409 Conflict) nếu đã có sản phẩm/bảng giá/biến thể liên kết.
    - Cung cấp các endpoint Public GET phục vụ dropdown chọn thương hiệu, đơn vị tính, màu sắc (`/api/v1/brands`, `/api/v1/units`, `/api/v1/colors`).
  - `Product`, `ProductImage`, `ProductVariant`, `ProductColor`, `ProductPrice`:
    - `Product`: Quản lý thông tin chung, Brand, Category, trạng thái (`ACTIVE`, `INACTIVE`), người tạo.
    - `ProductImage`: Ràng buộc tối đa 1 ảnh đại diện (`isPrimary = true`), tối đa 10 ảnh phụ (tổng tối đa 11 ảnh cho 1 sản phẩm).
    - `ProductVariant`: Thông số kỹ thuật sản xuất (`typeValue`, `thickness`), mã biến thể duy nhất, màu sắc liên kết qua `ProductColor`.
    - `ProductPrice`: Ánh xạ giá bán linh hoạt theo đơn vị tính (`(variantId, unitId) -> price`), tính theo số nguyên VND (`INT`/`BIGINT`).
  - Bảo mật & Phân quyền hiển thị giá (Price Visibility):
    - Sử dụng quyền động `PRODUCT_PRICE_VIEW` kiểm tra qua `PermissionEvaluationService` (tự động lấy Authentication từ SecurityContext).
    - Khi caller thiếu quyền: ẩn giá (`prices = null`), chặn sắp xếp theo giá (`sortBy=price` ném ngoại lệ `BusinessRuleException` tránh rò rỉ thông tin giá gián tiếp).
  - Ràng buộc toàn vẹn & Cơ chế Xóa mềm (Soft Delete):
    - `Category`: Nếu rỗng -> xóa mềm ngay; nếu chứa con/sản phẩm và `force=false` -> trả về 409 Conflict kèm payload số lượng con (`ConflictWithPayloadException`) để frontend hiển thị confirm popup; nếu `force=true` -> xóa mềm cascade nếu không có sản phẩm nào nằm trong đơn hàng dở dang.
    - `Product`: Cho phép xóa mềm ngay cả khi có đơn hàng lịch sử (`COMPLETED`/`CANCELLED`); chỉ chặn xóa (409 Conflict) khi sản phẩm đang nằm trong đơn hàng chưa hoàn thành (`PENDING`, `CONFIRMED`, `PROCESSING`, `SHIPPING`).
  - Dữ liệu mẫu thực tế trong `DataInitializer`:
    - Seed đầy đủ quyền catalog cho `ADMIN` và `MANAGER`.
    - Khởi tạo thương hiệu tôn (Hoa Sen, Đông Á, Nam Kim), đơn vị tính (m, tấm, cuộn, cái), màu sắc (Xanh ngọc, Đỏ đậm, Xanh rêu,...), cây danh mục tôn lợp mái, phụ kiện và sản phẩm mẫu có biến thể, bảng giá.
  - Toàn bộ 157/157 tests pass (thêm 57 test cases mới cho Phase 3 bao gồm Service tests và Controller MockMvc tests).
- ✅ **Phase 2 — Authentication, Tokens & OTP completed**:
  - `GoogleAuthService` & `GoogleAuthServiceImpl`: Xác thực Google ID Token qua endpoint Google TokenInfo (`https://oauth2.googleapis.com/tokeninfo`) sử dụng `RestClient`.
  - `JwtTokenService` & `JwtTokenServiceImpl`: Cấp phát JWT Access Token (HMAC-SHA256) chứa claims `sub`, `email`, `roles`, `jti`, `exp` (bỏ qua `permissions` để hỗ trợ phân quyền động in-memory), tự động đăng ký `auth:access:{jti}` vào Redis với TTL.
  - `RefreshToken` entity & `RefreshTokenRepository`: Lưu trữ mã băm SHA-256 của Refresh Token, quản lý `tokenFamilyId`, `jti`, thời hạn hết hạn `expiresAt` và cờ thu hồi `isRevoked`.
  - `RefreshTokenService` & `RefreshTokenServiceImpl`: Triển khai xoay vòng Refresh Token (Token Rotation) nguyên tử, phát hiện tấn công tái sử dụng (Replay Attack) và thu hồi toàn bộ Token Family.
  - `AuthService` & `AuthServiceImpl`: Điều phối toàn bộ luồng đăng nhập Google (idempotent user creation cho CUSTOMER, chặn tài khoản LOCKED), làm mới token và đăng xuất (xóa cookie HttpOnly, xóa Redis `auth:access:{jti}`, đánh dấu thu hồi trong DB).
  - `AuthController`: Các endpoint RESTful `/api/v1/auth/google`, `/api/v1/auth/refresh`, `/api/v1/auth/logout` với transport cookie `HttpOnly; Secure; SameSite=Lax`.
  - `PhoneOtpService` & `PhoneOtpServiceImpl`: Sinh mã OTP 6 chữ số ngẫu nhiên lưu Redis với TTL 5 phút, giới hạn tần suất gửi lại (cooldown 60s) và giới hạn số lần nhập sai tối đa (5 lần), lưu bảo chứng xác thực `otp:verified:{phone}` (TTL 10 phút) dùng khi đặt đơn hàng.
  - `OtpController`: Endpoint RESTful `/api/v1/orders/otp/request`, `/api/v1/orders/otp/verify` và `/api/v1/me/otp/**`.
  - Bổ sung quyền `ORDER_OTP_REQUEST`, `ORDER_OTP_VERIFY`, `ME_OTP_REQUEST`, `ME_OTP_VERIFY` vào `DataInitializer` cho vai trò `CUSTOMER`.
  - Toàn bộ 100/100 tests pass (thêm 35 test cases mới cho Phase 2: `JwtTokenServiceImplTest`, `RefreshTokenServiceImplTest`, `AuthServiceImplTest`, `PhoneOtpServiceImplTest`, `AuthControllerTest`, `OtpControllerTest`).
- ✅ **Phase 1 — Identity, Roles & Permissions (Task 1 & Task 2 completed)**:
  - JPA Entities configured in `com.hmp.sales.feature.user.entity`:
    - `Role` (bảng `roles`, khóa chính `role_id`, ràng buộc duy nhất trên `role_code`, cờ `is_deleted`, audit timestamps).
    - `Permission` (bảng `permissions`, khóa chính `permission_id`, `permission_name` duy nhất, ràng buộc `(api_path, method)`, phân hệ `module`, cờ `is_deleted`, audit timestamps).
    - `RolePermission` (bảng liên kết `role_permissions`, composite key `RolePermissionId`, hỗ trợ phân quyền động `PRODUCT_PRICE_VIEW`).
    - `User` (bảng `users`, khóa chính `user_id`, hỗ trợ `GOOGLE` và `LOCAL`, `ACTIVE`/`LOCKED`, ràng buộc duy nhất `email` và `(auth_provider, provider_subject)`).
  - Enums: `RoleCode` (`ADMIN`, `MANAGER`, `CUSTOMER`), `AuthProvider` (`GOOGLE`, `LOCAL`), `UserStatus` (`ACTIVE`, `LOCKED`).
  - Spring Data JPA Repositories in `com.hmp.sales.feature.user.repository`:
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
  - Refactor: Triển khai kiến trúc **Dynamic In-Memory Permission Authorization**:
    - `PermissionAuthorizationManager` implements `AuthorizationManager<RequestAuthorizationContext>`: nạp cache singleton `rolePermissionsCache` vào RAM khi khởi động.
    - So khớp quyền động qua `AntPathMatcher` dựa trên `(apiPath, method)` của quyền thuộc vai trò trong cache.
    - Tích hợp `anyRequest().access(permissionAuthorizationManager)` vào `SecurityConfig`.
    - Tự động làm mới cache (cache invalidation) qua `loadCache()` trong `RoleServiceImpl` và `DataInitializer`.
    - Cập nhật `PermissionEvaluationService` tra cứu quyền động từ cache RAM cho các logic nghiệp vụ lọc dữ liệu (như `PRODUCT_PRICE_VIEW`).
  - Toàn bộ 65/65 unit và integration tests pass (11 test cases mới: `PermissionAuthorizationManagerTest`, `RolePermissionRepositoryTest`, `PermissionEvaluationServiceTest`).

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
1. **[P1]** Implement Phase 4: Customer Address entity, repository, and CRUD APIs (`GET/POST/PATCH/DELETE /api/v1/me/addresses`).
2. **[P1]** Implement Phase 4: Customer profile APIs (`GET /api/v1/me`, `PATCH /api/v1/me`).
3. **[P1]** Implement Phase 4: Order & OrderItem entities with immutable price snapshots (`UnitPrice`, `TotalPrice`, `TotalAmount`).
4. **[P1]** Implement Phase 4: Customer order creation with phone OTP verification & direct PostgreSQL persistence (`PENDING` state).
5. **[P1]** Implement Phase 4: Manager order calculation (`TotalPrice = UnitPrice × Size × Quantity`) and lifecycle transitions (`PENDING -> CONFIRMED -> PROCESSING -> COMPLETED`).

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
- [x] Google OAuth token verification service & user account resolution (idempotent mapping)
- [x] Application JWT token generation service (with unique `jti`, roles claim, dynamic permissions)
- [x] Redis active access token registry (`auth:access:{jti}` with TTL)
- [x] RefreshToken entity & repository with cryptographic hash storage, family tracking, and rotation
- [x] Refresh token rotation logic with token reuse detection and revocation
- [x] Auth endpoints:
  - [x] `POST /api/v1/auth/google` (login / register with Google)
  - [x] `POST /api/v1/auth/refresh` (rotate refresh token, issue new JWT)
  - [x] `POST /api/v1/auth/logout` (revoke Redis access token and PostgreSQL refresh token)
- [x] Redis phone OTP service:
  - [x] 6-digit random OTP generation with TTL (`otp:phone:<phone>`)
  - [x] Rate limiting for OTP generation and verification attempts
  - [x] OTP verification and single-use invalidation
- [x] Auth & OTP unit and integration tests (100/100 tests pass)

### Phase 3 — Catalog & Category Hierarchy
- [x] Category entity (self-referencing parent-child tree) & repository
- [x] Category tree validation (prevent circular hierarchy references)
- [x] Master data entities & CRUD repositories:
  - [x] Brand entity & repository (`ACTIVE`/`LOCKED`, soft-delete)
  - [x] Unit entity & repository (`ACTIVE`/`LOCKED`, soft-delete)
  - [x] Color entity & repository (`ACTIVE`/`LOCKED`, soft-delete)
- [x] Product entity (`ACTIVE`/`INACTIVE` status, Brand, Category references)
- [x] ProductImage entity (1 primary image constraint, max 10 secondary images)
- [x] ProductVariant entity (spec, thickness, variantCode, active state)
- [x] ProductColor entity & composite repository
- [x] ProductPrice entity & composite repository (by variant and unit, VND integers)
- [x] Product status management (active/locking logic: prevent ordering inactive products)
- [x] Public catalog & master data APIs:
  - [x] `GET /api/v1/categories/tree` (hierarchical category tree)
  - [x] `GET /api/v1/categories/{categoryId}/products` (products in category)
  - [x] `GET /api/v1/brands`, `GET /api/v1/units`, `GET /api/v1/colors` (active master data for browsing/filters)
  - [x] `GET /api/v1/products` (filter, sort, search, conditional price visibility based on `PRODUCT_PRICE_VIEW`)
  - [x] `GET /api/v1/products/{productId}` (detail, variants, images, conditional price visibility)
- [x] Manager catalog management APIs:
  - [x] Create child category or product under current category
  - [x] Category CRUD & display order updates
  - [x] Product CRUD, image management, variant management
  - [x] Product lock/unlock endpoint
- [x] Admin master data management APIs (Dynamic CRUD for Brand, Unit, Color):
  - [x] Brand CRUD, search/pagination, lock/unlock (`/api/v1/admin/brands/**`)
  - [x] Unit CRUD, search/pagination, lock/unlock (`/api/v1/admin/units/**`)
  - [x] Color CRUD, search/pagination, lock/unlock (`/api/v1/admin/colors/**`)
- [x] Catalog unit and integration tests (157/157 tests pass)

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
