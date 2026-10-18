# Database Design — PostgreSQL

## 1. Design principles

PostgreSQL is the authoritative persistence layer for all durable business data.

Redis is not a replacement for PostgreSQL. It is reserved for temporary state, TTL data (OTP, active access tokens), rate limiting, and optional cache. Orders are persisted and processed directly in PostgreSQL.

Historical order data must remain stable when mutable master data changes.

## 2. Entity relationship overview

```text
User
 ├──< Role
 │        │
 │        └──< RolePermission >── Permission
 │
 ├──< CustomerAddress
 ├──< RefreshToken
 ├──< Order
 ├──< Notification
 └──< SystemLog

Category
 └──< Category (parent-child)
      
Category
 └──< Product
       ├──< ProductImage
       ├──< ProductVariant ──< ProductPrice >── Unit
                     └──< ProductColor >── Color

Brand ──< Product

Product ──< OrderItem >── Order
Order ──< OrderStatusHistory

SystemSetting
```

## 3. Table: Role

| Column | PostgreSQL | Null | Notes |
|---|---|---:|---|
| RoleID | INT IDENTITY | No | PK |
| RoleCode | VARCHAR(30) | No | `ADMIN`, `MANAGER`, `CUSTOMER` |
| RoleName | NVARCHAR(100) | No | Display name |
| Description | NVARCHAR(500) | Yes | Optional |
| IsDeleted | BOOLEAN | No | Default false; soft delete flag |

Unique constraint on `RoleCode`.

## 4. Table: User

Purpose: all application identities.

| Column | PostgreSQL | Null | Notes |
|---|---|---:|---|
| UserID | BIGINT IDENTITY | No | PK |
| FullName | NVARCHAR(150) | No | Display name |
| Email | NVARCHAR(255) | No | Unique for application account |
| Phone | VARCHAR(20) | Yes | Customer contact; can be supplied later |
| PasswordHash | NVARCHAR(255) | Yes | Null for Google-authenticated Customer |
| AuthProvider | VARCHAR(30) | No | `GOOGLE`, `LOCAL` |
| ProviderSubject | NVARCHAR(255) | Yes | Stable Google subject/identity key |
| Status | VARCHAR(20) | No | `ACTIVE`, `LOCKED` |
| CreatedAt | DATETIME2 | No | Default current UTC/application time policy |
| UpdatedAt | DATETIME2 | No | Updated timestamp |
| RoleID | INT | No | FK |
| IsDeleted | BOOLEAN | No | Default false; soft delete flag |

Recommended uniqueness:
- `Email` unique.
- `(AuthProvider, ProviderSubject)` unique when ProviderSubject is not null.

Customer forgot-password/change-password endpoints are not part of this project for Google accounts.

Access tokens are JWTs and are stored only in Redis while active. Refresh tokens are stored durably in PostgreSQL and rotated.

## 5. Table: Permission

Permission-driven authorization allows Admin to expose product prices later without changing application code.

Recommended permissions include:

```text
PRODUCT_PRICE_VIEW
PRODUCT_VIEW
PRODUCT_CREATE
PRODUCT_UPDATE
PRODUCT_LOCK
PRODUCT_CATEGORY_CREATE
PRODUCT_CATEGORY_UPDATE
PRODUCT_CATEGORY_DELETE
ORDER_CREATE
ORDER_VIEW_OWN
ORDER_CANCEL_OWN
ORDER_CONFIRM_OWN
ORDER_VIEW_ALL
ORDER_CALCULATE
ORDER_CANCEL_ANY
ORDER_STATUS_UPDATE
CUSTOMER_VIEW_ALL
USER_LOCK
USER_UNLOCK
MANAGER_CREATE
SYSTEM_LOG_VIEW
SYSTEM_SETTING_UPDATE
ROLE_PERMISSION_UPDATE
```

The exact permission catalog can grow, but each permission should represent an actual server-side authorization decision.

| Column | PostgreSQL | Null | Notes |
|---|---|---:|---|
| PermissionID | INT IDENTITY | No | PK |
| PermissionName | NVARCHAR(150) | No | Display name |
| Api_path | NVARCHAR(255) | No | e.g. /api/v1/users |
| Method | VARCHAR(10) | NOT NULL | GET, POST, PUT, DELETE |
| Module | VARCHAR(100) | NOT NULL | e.g. USER, COMPANY, ROLE |
| Created_at | DATETIME2 | NOT NULL, DEFAULT CURRENT_DATETIME | |
| Updated_at | DATETIME2 | NULLABLE, ON UPDATE CURRENT_DATETIME | |
| Description | NVARCHAR(500) | Yes | Optional |
| IsDeleted | BOOLEAN | No | Default false; soft delete flag |

## 6. Table: RolePermission

| Column | PostgreSQL | Null | Notes |
|---|---|---:|---|
| RoleID | INT | No | FK Role |
| PermissionID | INT | No | FK Permission |
| IsDeleted | BOOLEAN | No | Default false; soft delete flag |

PK: `(RoleID, PermissionID)`.

Admin changes to this table should be audited in SystemLog.

## 7. Table: Category

This is a self-referencing tree.

| Column | PostgreSQL | Null | Notes |
|---|---|---:|---|
| CategoryID | BIGINT IDENTITY | No | PK |
| ParentCategoryID | BIGINT | Yes | FK Category.CategoryID; null = root |
| CategoryName | NVARCHAR(150) | No | Name |
| Status | VARCHAR(20) | No | `ACTIVE`, `INACTIVE` |
| CreatedBy | BIGINT | No | FK User |
| CreatedAt | DATETIME2 | No | Created timestamp |
| UpdatedAt | DATETIME2 | No | Updated timestamp |
| IsDeleted | BOOLEAN | No | Default false; soft delete flag |

Rules:
- Root category has null ParentCategoryID.
- A category cannot be its own parent.
- Application logic must prevent cycles when moving categories.
- If categories support moving later, perform cycle checks before update.

## 8. Table: Brand

| Column | PostgreSQL | Null | Notes |
|---|---|---:|---|
| BrandID | BIGINT IDENTITY | No | PK |
| BrandName | NVARCHAR(150) | No | Unique |
| Status | VARCHAR(20) | No | `ACTIVE`, `INACTIVE` |
| CreatedAt | DATETIME2 | No | |
| UpdatedAt | DATETIME2 | No | |
| IsDeleted | BOOLEAN | No | Default false; soft delete flag |

The business requirement includes Brand as common product information but does not define a full Brand-management workflow. CRUD scope is `TBD`.

## 9. Table: Unit

Admin-configurable selling unit.

| Column | PostgreSQL | Null | Notes |
|---|---|---:|---|
| UnitID | BIGINT IDENTITY | No | PK |
| UnitName | NVARCHAR(50) | No | `m`, `cái`, `tấm`, etc. |
| Status | VARCHAR(20) | No | `ACTIVE`, `INACTIVE` |
| CreatedAt | DATETIME2 | No | |
| UpdatedAt | DATETIME2 | No | |
| IsDeleted | BOOLEAN | No | Default false; soft delete flag |

Unique `UnitName`.

## 10. Table: Product

| Column | PostgreSQL | Null | Notes |
|---|---|---:|---|
| ProductID | BIGINT IDENTITY | No | PK |
| CategoryID | BIGINT | No | FK Category |
| BrandID | BIGINT | Yes | FK Brand |
| ProductName | NVARCHAR(250) | No | Product name |
| Description | NVARCHAR(MAX) | Yes | Product description |
| Status | VARCHAR(20) | No | `ACTIVE`, `INACTIVE` |
| CreatedBy | BIGINT | No | FK User |
| CreatedAt | DATETIME2 | No | |
| UpdatedAt | DATETIME2 | No | |
| IsDeleted | BOOLEAN | No | Default false; soft delete flag |

`INACTIVE` means the product is locked/unavailable for new orders.

Do not physically delete a product that is referenced by historical OrderItem rows. Prefer locking/deactivation.

## 11. Table: ProductImage

Requirement: one primary image and up to 10 secondary images.

| Column | PostgreSQL | Null | Notes |
|---|---|---:|---|
| ProductImageID | BIGINT IDENTITY | No | PK |
| ProductID | BIGINT | No | FK Product |
| ImageUrl | NVARCHAR(1000) | No | Cloud/object storage URL |
| IsPrimary | BOOLEAN | No | Exactly one primary for a completed product |
| DisplayOrder | INT | No | Ordering |
| CreatedAt | DATETIME2 | No | |
| IsDeleted | BOOLEAN | No | Default false; soft delete flag |

Application/service rules:
- Maximum 11 total images per Product in V1.
- Maximum one primary image.
- Enforce one-primary invariant transactionally/application-side; if supported by the chosen schema strategy, add a filtered unique index for `IsPrimary = 1`.

## 12. Table: Color

| Column | PostgreSQL | Null | Notes |
|---|---|---:|---|
| ColorID | BIGINT IDENTITY | No | PK |
| ColorName | NVARCHAR(100) | Yes | Manufacturing color |
| IsDeleted | BOOLEAN | No | Default false; soft delete flag |

## 13. Table: ProductVariant

The requirements state that products, especially manufacturing products, can have multiple variants. V1 explicitly needs manufacturing attributes such as type, color and thickness.

| Column | PostgreSQL | Null | Notes |
|---|---|---:|---|
| VariantID | BIGINT IDENTITY | No | PK |
| ProductID | BIGINT | No | FK Product |
| VariantCode | VARCHAR(80) | Yes | Internal/business code if needed |
| VariantName | NVARCHAR(150) | No | Human-readable variant |
| TypeValue | NVARCHAR(100) | Yes | Manufacturing type |
| Thickness | DECIMAL(10,3) | Yes | mm; exact precision/range TBD |
| VariantPrice | DECIMAL(19,4) | Yes | Variant-level price when applicable |
| Status | VARCHAR(20) | No | `ACTIVE`, `INACTIVE` |
| CreatedAt | DATETIME2 | No | |
| UpdatedAt | DATETIME2 | No | |
| IsDeleted | BOOLEAN | No | Default false; soft delete flag |

Important: `Size/Length` supplied by the customer during a manufacturing order is not stored as a static variant attribute. It belongs to the OrderItem snapshot.

## 14. Table: ProductPrice

| Column | PostgreSQL | Null | Notes |
|---|---|---:|---|
| VariantID | INT | No | FK ProductVariant |
| UnitID | INT | No | FK Unit |
| Price | INT | No | Price of product by variant |
| IsDeleted | BOOLEAN | No | Default false; soft delete flag |

PK: `(VariantID, UnitID)`.

## 15. Table: ProductColor

| Column | PostgreSQL | Null | Notes |
|---|---|---:|---|
| VariantID | INT | No | FK ProductVariant |
| ColorID | INT | No | FK Color |
| IsDeleted | BOOLEAN | No | Default false; soft delete flag |

PK: `(VariantID, ColorID)`.

## 16. Table: CustomerAddress

Saved addresses selectable during ordering.

| Column | PostgreSQL | Null | Notes |
|---|---|---:|---|
| AddressID | BIGINT IDENTITY | No | PK |
| CustomerID | BIGINT | No | FK User |
| AddressLine | NVARCHAR(500) | No | Full address |
| IsDefault | BOOLEAN | No | Default selection |
| Status | VARCHAR(20) | No | `ACTIVE`, `INACTIVE` |
| CreatedAt | DATETIME2 | No | |
| UpdatedAt | DATETIME2 | No | |
| IsDeleted | BOOLEAN | No | Default false; soft delete flag |

Customer may save multiple addresses. Keep order delivery information separate from mutable address records.

## 17. Table: RefreshToken

Refresh tokens are durable authentication-session records. The plaintext refresh token is returned only to the client at issuance time; PostgreSQL stores a cryptographic hash for lookup/revocation.

| Column | PostgreSQL | Null | Notes |
|---|---|---:|---|
| RefreshTokenID | BIGINT IDENTITY | No | PK |
| UserID | BIGINT | No | FK User |
| TokenFamilyID | UNIQUEIDENTIFIER | No | Token-rotation family/session |
| TokenHash | VARBINARY(32) | No | SHA-256 hash of the opaque refresh token |
| AccessTokenJti | UNIQUEIDENTIFIER | No | JTI of the currently linked access token |
| ExpiresAt | DATETIME2 | No | |
| IsRevoked | BOOLEAN | DEFAULT False | Set the token is revoked/rotated |
| CreatedAt | DATETIME2 | No | |
| IsDeleted | BOOLEAN | No | Default false; soft delete flag |

Rules:
- Only one active use of a refresh token is allowed.
- On successful refresh, the old token is revoked/consumed and a new row is created.
- The new row belongs to the same `TokenFamilyID`.
- A reused/replayed revoked token may trigger revocation of the whole token family according to the final security policy.
- Do not store plaintext refresh-token values in PostgreSQL.
- Keep refresh-token records even after rotation until the retention/cleanup policy permits deletion.

## 18. Table: Orders

Avoid the SQL keyword `Order` as an unquoted physical table name if the project convention considers it reserved/confusing. Recommended physical name: `Orders`.

| Column | PostgreSQL | Null | Notes |
|---|---|---:|---|
| OrderID | BIGINT IDENTITY | No | PK |
| CustomerID | BIGINT | No | FK User |
| Status | VARCHAR(30) | No | `PENDING`, `CONFIRMED`, `PROCESSING`, `COMPLETED`, `CANCELLED` |
| TotalAmount | BIGINT | Yes | Null until price calculation is complete when applicable |
| ReceiverName | NVARCHAR(150) | No | Historical snapshot |
| ReceiverPhone | VARCHAR(20) | No | Historical snapshot |
| ShippingAddress | NVARCHAR(1000) | No | Historical snapshot |
| Note | NVARCHAR(1000) | Yes | Customer note |
| CancellationReason | NVARCHAR(500) | Yes | Optional for every role |
| CancelledBy | BIGINT | Yes | FK User |
| CancelledAt | DATETIME2 | Yes | |
| CalculatedBy | BIGINT | Yes | FK User; typically Manager |
| CalculatedAt | DATETIME2 | Yes | |
| ConfirmedAt | DATETIME2 | Yes | |
| CompletedAt | DATETIME2 | No | Only if IsCompleted = true|
| CreatedAt | DATETIME2 | No | |
| UpdatedAt | DATETIME2 | No | |
| IsDeleted | BOOLEAN | No | Default false; soft delete flag |

The order is the durable business record. Orders are persisted and managed directly in PostgreSQL without an external Redis order queue.

## 19. Table: OrderItem

| Column | PostgreSQL | Null | Notes |
|---|---|---:|---|
| OrderItemID | BIGINT IDENTITY | No | PK |
| OrderID | BIGINT | No | FK Orders |
| ProductID | BIGINT | No | FK Product |
| VariantID | BIGINT | Yes | FK ProductVariant |
| UnitID | BIGINT | Yes | FK Unit |
| Quantity | DECIMAL(18,4) | No | For future unit flexibility; integer-only enforcement may be business-specific |
| Size | DECIMAL(18,4) | Yes | Customer-entered length/size for size-based products |
| UnitPrice | INT | Yes | Price snapshot after Manager calculation |
| TotalPrice | BIGINT | Yes | Calculated line total |
| ColorSnapshot | NVARCHAR(100) | Yes | Preserve customer-selected color |
| ProductNameSnapshot | NVARCHAR(250) | No | Protect order history from product rename |
| UnitNameSnapshot | NVARCHAR(50) | Yes | Protect unit display history |
| IsDeleted | BOOLEAN | No | Default false; soft delete flag |

For current size-based manufacturing calculation:

```text
TotalPrice = UnitPrice × Size × Quantity
```
Note:
For production categories, the size will be null; in this case, the calculation formula will be: 
TotalPrice = UnitPrice × Size × Quantity

Store the calculation result so later master-price changes do not alter completed/historical orders.

## 20. Table: OrderStatusHistory

| Column | PostgreSQL | Null | Notes |
|---|---|---:|---|
| HistoryID | BIGINT IDENTITY | No | PK |
| OrderID | BIGINT | No | FK Orders |
| FromStatus | VARCHAR(30) | Yes | Null for first state |
| ToStatus | VARCHAR(30) | No | New state |
| ChangedBy | BIGINT | No | FK User |
| Reason | NVARCHAR(500) | Yes | Optional |
| CreatedAt | DATETIME2 | No | |
| IsDeleted | BOOLEAN | No | Default false; soft delete flag |

Every status transition must be recorded.

## 21. Table: Notification

For in-website notification history.

| Column | PostgreSQL | Null | Notes |
|---|---|---:|---|
| NotificationID | BIGINT IDENTITY | No | PK |
| UserID | BIGINT | No | Recipient |
| Type | VARCHAR(50) | No | e.g. `ORDER_STATUS`, `ORDER_CALCULATED` |
| Title | NVARCHAR(250) | No | |
| Message | NVARCHAR(MAX) | No | |
| RelatedEntityType | VARCHAR(50) | Yes | e.g. `ORDER` |
| RelatedEntityID | BIGINT | Yes | e.g. OrderID |
| IsRead | BOOLEAN | No | Default False |
| CreatedAt | DATETIME2 | No | |
| ReadAt | DATETIME2 | Yes | |
| IsDeleted | BOOLEAN | No | Default false; soft delete flag |

Zalo/email delivery should not require duplicating the notification content into separate durable business tables unless a provider audit requirement appears.

## 22. Table: SystemLog

Audit trail for administrative and important business actions.

| Column | PostgreSQL | Null | Notes |
|---|---|---:|---|
| SystemLogID | BIGINT IDENTITY | No | PK |
| UserID | BIGINT | Yes | Actor |
| Action | VARCHAR(100) | No | Machine-readable action |
| EntityType | VARCHAR(50) | Yes | Entity affected |
| EntityID | BIGINT | Yes | Entity ID |
| Description | NVARCHAR(MAX) | Yes | Human-readable explanation |
| OldValue | NVARCHAR(MAX) | Yes | Before state / JSON-like text |
| NewValue | NVARCHAR(MAX) | Yes | After state / JSON-like text |
| CreatedAt | DATETIME2 | No | |
| IsDeleted | BOOLEAN | No | Default false; soft delete flag |

Never store passwords, JWTs, OTP values, OAuth tokens, or other secrets in OldValue/NewValue.

## 23. Table: SystemSetting

Admin-configurable parameters.

| Column | PostgreSQL | Null | Notes |
|---|---|---:|---|
| SettingID | INT IDENTITY | No | PK |
| SettingKey | VARCHAR(100) | No | Unique key |
| SettingValue | NVARCHAR(1000) | No | Serialized value |
| DataType | VARCHAR(30) | No | `INT`, `BOOLEAN`, `DECIMAL`, `STRING`, etc. |
| Description | NVARCHAR(500) | Yes | |
| UpdatedBy | BIGINT | Yes | FK User |
| UpdatedAt | DATETIME2 | No | |
| IsDeleted | BOOLEAN | No | Default false; soft delete flag |

Initial settings can include:

```text
ORDER_CUSTOMER_CANCELLATION_WINDOW_MINUTES
```

Additional settings should be added only when an explicit requirement exists.

## 24. Relationships

```text
Role 1---N User
Role 1---N RolePermission N---1 Permission

User 1---N CustomerAddress
User 1---N RefreshToken
User 1---N Order
User 1---N Notification
User 1---N SystemLog

Category 1---N Category
Category 1---N Product
Brand 1---N Product
Product 1---N ProductVariant
Unit 1---N ProductPrice N---1 ProductVariant 
Color 1---N ProductColor N---1 ProductVariant
Product 1---N ProductImage


Order 1---N OrderItem
Product 1---N OrderItem
ProductVariant 1---N OrderItem
Order 1---N OrderStatusHistory
```

## 25. Key indexes

Recommended starting indexes:

```text
User:          UX_User_Email
User:          UX_User_Provider
RefreshToken:  UX_RefreshToken_Hash
RefreshToken:  IX_RefreshToken_User_ExpiresAt
RefreshToken:  IX_RefreshToken_Family
Category:      IX_Category_Parent_Status
Product:       IX_Product_Category_Status
Product:       IX_Product_Status
Product:       IX_Product_Name (or full-text strategy later)
Variant:       IX_ProductVariant_Product_Status
Order:         IX_Order_Customer_CreatedAt
Order:         IX_Order_Status_CreatedAt
OrderItem:     IX_OrderItem_Order
History:       IX_OrderStatusHistory_Order_CreatedAt
Notification:  IX_Notification_User_IsRead_CreatedAt
SystemLog:     IX_SystemLog_User_CreatedAt
SystemLog:     IX_SystemLog_Entity
Setting:       UX_SystemSetting_Key
```

Exact index strategy should be confirmed from query plans after implementation rather than creating every possible index upfront.

## 26. Delete/update policy

- Soft delete: all durable tables include IsDeleted (BOOLEAN, default alse) for soft-deletion without breaking foreign key references or audit history.
- Product: prefer status change over physical delete when referenced.
- Category: do not delete a non-empty category without a defined child/product migration policy.
- User: Admin can soft-delete a user by setting `IsDeleted = true`. Physical deletion is never performed. Historical order records remain intact and readable. Active access tokens (Redis) and all refresh tokens (PostgreSQL) for the deleted user are revoked at deletion time.
- Order: never hard-delete ordinary business orders from normal admin flows.
- OrderItem snapshots protect historical information.

## 27. Redis data design

Redis keys should be namespaced.

Examples:

```text
auth:access:{jti}
auth:otp:phone:{phone}
notification:queue:v1
cache:product:{productId}
```

### Access tokens

The issued JWT access token is stored in Redis while active.

Example:

```text
Key:   auth:access:{jti}
Value: <JWT access token>
TTL:   access-token expiration - current time
```

The API authentication path validates the JWT and then checks this Redis key. Missing/expired Redis state means the access token is rejected even if its JWT signature is otherwise valid.

When the access token is refreshed or the user logs out, its Redis entry is removed/revoked.

### OTP

OTP should have a short TTL. Exact TTL is configed by Admin.

