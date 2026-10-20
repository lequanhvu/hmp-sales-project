# Architecture — Sales Website

## 1. Purpose

This document defines the high-level architecture and the main backend business flows of the sales website.

The website combines direct online ordering with product introduction/contact. Guests can browse products, while login is required for ordering. The initial business scope is Vietnamese, individual customers, and one province/city.

## 2. Technology baseline

| Area | Technology |
|---|---|
| Language | Java |
| Backend | Spring Boot 4.x |
| API style | RESTful API |
| Security | Spring Security 7 |
| Authentication | Google login + application JWT bearer authentication |
| JWT protection | OAuth2 Resource Server |
| Database | PostgreSQL |
| Access-token state / temporary (OTP) | Redis |
| Testing | JUnit 5 |
| Source control | Git / GitHub |
| Development environment | Antigravity IDE |

Spring Security OAuth2 Resource Server supports JWT bearer-token validation; the backend should keep token authentication and business authorization concerns separate. The current Spring documentation describes bearer-token processing through the Resource Server security chain. [Spring Security reference](https://docs.spring.io/spring-security/reference/7.0/servlet/oauth2/resource-server/index.html)

## 3. Logical architecture

```text
                    Client / Frontend
                           |
                           | HTTPS REST/JSON
                           v
                  +---------------------+
                  |   Spring Security   |
                  | JWT Resource Server  |
                  +----------+----------+
                             |
                             v
                  +---------------------+
                  |    REST Controllers  |
                  +----------+----------+
                             |
                             v
                  +---------------------+
                  |      Services        |
                  | business rules       |
                  +----+------------+----+
                       |            |
                SQL data path   Redis path
                       |            |
                       v            v
               +-----------+   +------------------+
               |PostgreSQL |   |      Redis       |
               | source of |   | access JWT state |
               | truth     |   | OTP / cache      |
               | + refresh |   |                  |
               | tokens    |   |                  |
               +-----------+   +------------------+
                       |
                       v
               Notifications/
               external channels
               (Zalo / Email)
```

## 4. Core actors

### Guest
- Browse products.
- Search/filter/sort products.
- Open product detail.
- Contact the business.
- Must authenticate before ordering.

### Customer
- Everything a Guest can do.
- Maintain profile and saved addresses.
- Create and view own orders.
- Enter ordering information and OTP when phone verification is required.
- Cancel an eligible order.
- See calculated total and confirm the order.
- Receive website/Zalo/email notifications.

### Manager
- Manage category tree.
- Create child categories or products under a category.
- Manage product information, images, variants, unit, price, and status.
- View all customers.
- View and process orders.
- Calculate order price.
- Cancel orders.
- Update order status.

### Admin
- Manage users and Manager accounts.
- Lock/unlock accounts.
- View system logs.
- Configure units and other system settings.
- Manage role permissions.
- Control whether Customer has `PRODUCT_PRICE_VIEW` permission.

## 5. Main system flow

```text
Guest
  |
  +--> Browse/Search/Filter/Sort
  |
  +--> Login with Google
          |
          v
       Customer
          |
          +--> Create Order
                  |
                  v
              SQL: PENDING
                  |
                  +--------
                          |
                          v
                       Manager
                          |
                          +--> Calculate price
                          |      
                          |
                          +--> Notify Customer
                                   |
                                   v
                         Customer sees total
                                   |
                           Confirm or Cancel
                             /             \
                            v               v
                       CONFIRMED         CANCELLED
                           |
                           v
                      PROCESSING
                           |
                           v
                       SHIPPING
                           |
                           v
                       COMPLETED
```

## 6. Flow: product browsing

### 6.1 Business flow

```text
GET categories/tree
       |
       v
GET products?name=&categoryId=&status=&sort=
       |
       v
View product detail
```

Public customers should normally receive only active products. Product price is returned only when the caller has the `PRODUCT_PRICE_VIEW` permission.

### 6.2 Backend flow

```text
ProductController
   -> ProductQueryService
      -> validate filters/sort
      -> load active category/product data
      -> determine price visibility from authenticated authority
      -> ProductRepository
      -> ProductMapper
   -> ProductResponse
```

Never rely on frontend conditions to hide price. The API must omit or mask protected price data server-side.

## 7. Flow: category/product tree management

The catalog behaves like a file explorer.

```text
Category A
├── Category B
│   ├── Product 1
│   └── Product 2
└── Category C
    └── Product 3
```

When the Manager opens a category, the allowed actions are:

```text
[Create child category]
[Create product]
```

### Backend flow: create child category

```text
POST /manager/categories/{parentId}/children
 -> Security: MANAGER permission
 -> CategoryService
 -> verify parent exists and is usable
 -> prevent invalid self/cyclic relationships
 -> save Category(parentCategoryId = parentId)
 -> SystemLog
```

### Backend flow: create product

```text
POST /manager/categories/{categoryId}/products
 -> Security: MANAGER product-create permission
 -> verify category exists
 -> validate product data
 -> create Product
 -> create images/variants if supplied
 -> SystemLog
```

Category and Product remain separate tables. The tree is a UI/business representation assembled from `Category.parentCategoryId` and Product.categoryId.

## 8. Flow: Google authentication + application JWT

The business requirement specifies Google as the registration/login provider and requires authentication before ordering. Customer password-reset/change-password flows are not part of this project for Google accounts.

Proposed backend flow:

```text
Customer
  -> Google authentication
  -> frontend obtains Google credential/token
  -> backend authentication endpoint
  -> validate Google identity
  -> find existing User or create Customer
  -> issue application JWT
  -> client sends Authorization: Bearer <JWT>
  -> Spring Security OAuth2 Resource Server validates JWT
  -> application loads authorities/permissions
```

Exact Google OAuth client flow and application-token issuer configuration are `TBD` until the frontend and identity-provider responsibilities are finalized.

## 9. Flow: JWT access token + refresh token rotation

The application uses two token types:

| Token | Format | Storage | Lifetime | Purpose |
|---|---|---|---|---|
| Access Token | JWT | Redis (active token record) | Short-lived | Authenticate protected REST requests |
| Refresh Token | Opaque random token | PostgreSQL | Longer-lived | Obtain a new access token and a new refresh token |

The access token is validated by Spring Security OAuth2 Resource Server. In addition, the project must verify that the token is still registered/active in Redis. This means access-token authentication is not purely stateless.

### Login issuance flow

```text
Google identity
      |
      v
Validate Google credential
      |
      v
Find/create User
      |
      +--> issue Access Token (JWT, jti=A1)
      |
      +--> issue Refresh Token (opaque, id=R1)
      |
      +--> Redis SET auth:access:A1 = <access-token>
      |        TTL = access-token expiration
      |
      +--> SQL INSERT RefreshToken(R1, AccessTokenJti=A1)
      |
      v
Return Access Token + Refresh Token
```

### Refresh flow with rotation

```text
Client sends Refresh Token R1
            |
            v
Hash R1
            |
            v
Find active R1 in PostgreSQL
            |
            +--> not found / revoked / expired -> 401
            |
            v
Revoke R1
            |
            +--> remove/revoke its linked access token A1 in Redis
            |
            v
Issue Access Token A2
Issue Refresh Token R2
            |
            +--> Redis SET auth:access:A2 = <access-token>
            +--> SQL INSERT R2 with AccessTokenJti=A2
            |
            v
Return A2 + R2
```

Refresh-token rotation fields should support replay detection by linking each refresh token to the token family.

### Access-token validation flow

```text
Request
  |
  v
BearerTokenAuthenticationFilter
  |
  v
JWT validation (signature + claims)
  |
  v
Extract jti
  |
  v
Check Redis auth:access:{jti}
  |
  +--> missing/expired -> reject 401
  |
  v
Create authenticated SecurityContext
```

The Redis check is an application-specific requirement on top of normal OAuth2 Resource Server JWT validation.

### Logout/revocation

```text
Logout request
   |
   +--> remove current access token from Redis
   +--> revoke current refresh token in PostgreSQL
   +--> optionally revoke its token family when required
```

Access-token storage in Redis must use TLS/authentication/ACLs appropriate for production. Tokens must never be written to application logs.

The exact access-token TTL is 15 minutes, refresh-token TTL is 7 days

Cookie/header transport for the refresh token, and token-family replay policy remain `TBD`.

## 10. Flow: OTP

OTP is required when the customer enters a phone number during ordering.

```text
Customer enters phone
        |
POST /orders/otp/request
        |
Generate OTP
        |
Redis SET with TTL
        |
Send OTP through configured channel/provider
        |
Customer enters OTP
        |
POST /orders/otp/verify
        |
Redis GET + validate + consume
        |
Continue order submission
```

Redis is the temporary store. OTP values must not be persisted as ordinary business records in PostgreSQL unless a later audit requirement explicitly requires it.

## 11. Flow: create order

### Retail item

```text
Product + Quantity
```

### Manufacturing item (V1)

```text
Product: Tôn sóng lợp mái
Type
Color
Length/Size
Quantity
```

The requirement allows an order to contain products from different categories.

### Backend flow

```text
Customer
  -> POST /orders
  -> authenticate JWT
  -> authorize CUSTOMER order-create permission
  -> validate product is ACTIVE
  -> validate item input
  -> verify OTP when phone entry requires it
  -> create Order(PENDING)
  -> create OrderItem rows
  -> SQL transaction commits
  -> return order summary
```

## 12. Flow: Manager receives and calculates an order

```text
SQL order queue (today or pending status)
      |
      v
Manager order worklist
      |
      v
GET /manager/orders/{id}
      |
      v
POST /manager/orders/{id}/calculate
      |
      +--> load Product/Variant price
      +--> load OrderItem size + quantity
      +--> calculate
      |       Size-based formula:
      |       UnitPrice × Size × Quantity
      +--> snapshot UnitPrice and TotalPrice in OrderItem
      +--> calculate Order.TotalAmount
      +--> Save TotalAmount
      +--> commit transaction
```

For retail products, the requirement explicitly says the customer chooses product + quantity. The exact retail calculation formula is not separately specified; do not assume size-based multiplication unless the product's unit/business configuration requires it.

Ex: Unit: kg; UnitPrice: 5900; Size: 5kg -> TotalPrice = 5900 * 5

## 13. Flow: order cancellation

Allowed actors: Customer and Manager.

```text
Cancel request
   |
   +--> check caller role/permission
   +--> check current status is cancellable
   +--> if Customer: check configured cancellation window
   +--> cancellation reason OPTIONAL
   +--> set CANCELLED
   +--> save cancelledBy / cancelledAt
   +--> add OrderStatusHistory
```

`CancellationReason` is nullable for every role.

## 15. Flow: order status progression

```text
PENDING
  |
  +--> CANCELLED
  |
  v
CONFIRMED
  |
  +--> CANCELLED (only if business rule still permits)
  |
  v
PROCESSING
  |
  v
SHIPPING
  |
  v
COMPLETED
```

Every status change must create `OrderStatusHistory`.

## 16. Flow: product status / locking

```text
ACTIVE
  |
  | Manager locks product
  v
INACTIVE
```

`INACTIVE` means the product cannot be newly ordered. Historical OrderItem records remain valid.

Public product APIs should exclude inactive products by default. Manager/Admin APIs may query both states.

## 17. Flow: price visibility permission

The permission is:

```text
PRODUCT_PRICE_VIEW
```

Current intended state:

```text
ADMIN    -> allowed
MANAGER  -> allowed
CUSTOMER -> configurable by Admin; initially disabled
```

Authorization flow:

```text
Request product
 -> JWT authentication
 -> resolve authorities
 -> check PRODUCT_PRICE_VIEW
 -> include BasePrice / Variant price only when allowed
```

Do not implement this as a hard-coded `role == CUSTOMER` rule.

## 18. Flow: Dynamic In-Memory Permission Authorization & Cache Invalidation

To avoid token bloat (phình to token) and keep permissions instantly reactive to administrative changes, detailed permissions are **not** embedded into the JWT Access Token.

### Key architecture:
1. **JWT Access Token**: Only carries user identity (`sub`) and role authorities (e.g. `["ROLE_ADMIN"]`, `["ROLE_CUSTOMER"]`).
2. **Server-side RAM Cache (Singleton)**:
   - On application startup, `PermissionAuthorizationManager` loads all active `RolePermission` mappings into RAM (`rolePermissionsCache`).
   - Maps role codes (`ADMIN`, `ROLE_ADMIN`, `CUSTOMER`, `ROLE_CUSTOMER`) to their respective `List<Permission>` objects containing `(apiPath, method)`.
3. **Endpoint Access Evaluation (`PermissionAuthorizationManager`)**:
   - Implements Spring Security's `AuthorizationManager<RequestAuthorizationContext>`.
   - On every incoming request:
     - Extracts the user's roles from `Authentication`.
     - Extracts the request URI and HTTP method.
     - Uses `AntPathMatcher` to evaluate whether any of the user's role permissions match `(apiPath, method)`.
     - Allows access if matched, else returns `AuthorizationDecision(false)` -> 403 Forbidden.
4. **Data-level Permissions (e.g. `PRODUCT_PRICE_VIEW`)**:
   - Evaluated by `PermissionEvaluationService` dynamically querying `PermissionAuthorizationManager.hasPermission(auth, "PRODUCT_PRICE_VIEW")` against the in-memory cache without database queries.

### Cache Invalidation Flow (triggered on `PUT /api/v1/admin/roles/{roleId}/permissions`)

```text
Admin saves new permissions for Role R
         |
         v
 Atomically in one transaction:
   1. Replace RolePermission records for Role R (PostgreSQL)
   2. Log the change in SystemLog (PostgreSQL)
         |
         v
 Immediate Cache Invalidation:
   3. Call PermissionAuthorizationManager.loadCache() to reload RAM cache immediately.
      -> All subsequent requests for Role R instantly use updated permissions without waiting!
         |
         v
 Post-transaction cleanup:
   4. Find all active Users with Role R
         |
         v
   5. Revoke tokens if forced session termination is required
         |
         v
   6. Return 200 to Admin
```

### Stale permission prevention

Because endpoint authorization and data-masking evaluate against the singleton in-memory `rolePermissionsCache`, updating permissions via `loadCache()` guarantees that changes take effect immediately on the very next incoming HTTP request, solving the token-bloat and stale-token problems completely.

## 19. Flow: notifications

Required channels: website, Zalo, email.

Logical flow:

```text
Business event
   |
   v
NotificationService
   |
   +--> Email provider
   +--> Zalo provider
```

External provider credentials and message templates are configuration/secrets, never hard-coded.

## 20. Flow: system logging

Important admin audit events include account changes, product/category changes, order actions, permission changes, and other security-sensitive operations.

```text
Service business action
      |
      v
SystemLogService
      |
      v
PostgreSQL SystemLog
```

Logs must capture enough context to explain what changed, while excluding secrets and sensitive authentication material.

## 21. V1 exclusions

Do not implement these features unless the requirements change:
- Online payment
- Inventory/stock management
- Promotions, coupons, quantity-discount rules
- Product reviews
- Quotation module
- Laser cutting service
- Warranty workflow
- News/blog/user guides
- Full return workflow inside the system
- Excel/PDF reporting/export

The requirements explicitly place several of these items outside the first version.

## 22. Open decisions (`TBD`)

- Exact Google OAuth flow and application JWT issuer.
- Exact OTP delivery provider.
- Exact Zalo notification integration.
- Retail-product price calculation formula, if different from unit price × quantity.
- Exact definition/options of manufacturing `Type`.
- Allowed precision/range/unit for `Size/Length`.
- Maximum order quantity and validation constraints.
- Exact cancellable statuses and cancellation-duration semantics.
- Redis reliability strategy for guaranteed event publication after SQL commit.
- Cloud image storage/provider.
- Whether customer confirmation is required for every order type or only calculated manufacturing orders.

** Note: During the coding planning phase, if anything is unclear, you must ask for clarification before proceeding with the coding.
