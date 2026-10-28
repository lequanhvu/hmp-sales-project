# API Specification — Sales Website

## 1. API conventions

### 1.1 Base URL

```text
/api/v1
```

### 1.2 Transport

- HTTPS only in production.
- JSON is the default request/response format.
- All API responses use the same JSON response envelope.
- The HTTP status code and `statusCode` in the response body must match.

### 1.3 Response message convention

- `message` must always be written in Vietnamese.
- Success examples use Vietnamese messages such as `Thành công`, `Tạo đơn hàng thành công.`, or `Cập nhật sản phẩm thành công.`.
- Failure examples use Vietnamese messages that clearly describe the reason for failure.

### 1.4 Authentication

Protected APIs use JWT Bearer authentication through Spring Security OAuth2 Resource Server:

```http
Authorization: Bearer <access-token>
```

The application additionally checks the JWT `jti` against Redis for server-side access-token state and revocation.

Token storage contract:

```text
Access Token (JWT)
    -> Client: used as Bearer token
    -> Redis: auth:access:{jti}
    -> TTL: same lifetime as JWT expiration

Refresh Token (opaque random value)
    -> Client: HttpOnly + Secure cookie
    -> PostgreSQL: store only TokenHash
    -> Rotation: every successful refresh
```

The refresh token is not accepted as a Bearer token for ordinary business APIs.

### 1.5 Authorization

Authorization is always enforced server-side. Frontend visibility of buttons, menus, or URLs is not a security mechanism.

Examples:

```text
PRODUCT_PRICE_VIEW
PRODUCT_CREATE
PRODUCT_UPDATE
PRODUCT_DELETE
CATEGORY_CREATE
CATEGORY_UPDATE
CATEGORY_DELETE
USER_MANAGE
ROLE_PERMISSION_MANAGE
```

### 1.5 Resource state model

For business resources that support locking:

```text
ACTIVE -> LOCKED
LOCKED -> ACTIVE
```

`LOCK` is reversible through `UNLOCK`.

For business resources that support deletion:

```text
DELETE -> permanently removed
```

Permanent deletion has no restore operation. A deleted resource must be created again from the beginning.

Resources containing audit/security history must not use a destructive delete API unless that policy is explicitly approved. System logs are append-only.

---

## 2. Standard response contract

Every API response must use this structure. Do not add a `success` field.

### 2.1 Success response

```json
{
  "statusCode": 200,
  "data": {},
  "message": "Thành công",
  "timestamp": "20xx-02-28T10:00:00"
}
```

### 2.2 Failure response

```json
{
  "statusCode": 400,
  "data": null,
  "message": "Invalid request.",
  "timestamp": "20xx-02-28T10:00:00"
}
```

For validation errors, details may be placed inside `data`:

```json
{
  "statusCode": 400,
  "data": null,
  "errors": [
      {
        "field": "quantity",
        "message": "Quantity must be greater than 0."
      }
  ],
  "message": "Validation failed.",
  "timestamp": "20xx-02-28T10:00:00"
}
```

### 2.3 Standard status codes

| HTTP status | Meaning |
|---|---|
| 200 | Successful read or successful action with response data |
| 201 | Resource created successfully |
| 400 | Invalid request / validation failure |
| 401 | Missing or invalid authentication |
| 403 | Authenticated but not authorized |
| 404 | Resource not found |
| 409 | Business conflict / invalid current state |
| 500 | Unexpected server error |

---

# 3. Authentication APIs

## 3.1 Google login

```http
POST /api/v1/auth/google
```

Purpose: authenticate a Google identity and issue the application's tokens.

Google authentication is used for both `CUSTOMER` and `MANAGER` accounts. Admin creates/provisions Manager accounts by Google identity; Manager does not use a local password.

The exact Google credential/token payload is `TBD` because it depends on the frontend Google OAuth integration.

### Request example

```json
{
  "credential": "<google-credential-or-id-token>"
}
```

### Success response — 200

Refresh token is returned through `Set-Cookie`, not inside JSON.

```json
{
  "statusCode": 200,
  "data": {
    "accessToken": "<JWT>",
    "refreshToken": "eyJ..."
  },
  "message": "Thành công",
  "timestamp": "2026-10-01T10:00:00"
}
```

Response header:

```http
Set-Cookie: refresh_token=<opaque-token>; HttpOnly; Secure; SameSite=Lax; Path=/api/v1/auth
```

### Failure response — 401

```json
{
  "statusCode": 401,
  "data": null,
  "message": "Google authentication failed.",
  "timestamp": "2026-10-01T10:00:00"
}
```

---

## 3.2 Refresh access token

```http
POST /api/v1/auth/refresh
```

The refresh token is read from the secure HttpOnly cookie. Do not send the refresh token in the JSON request body.

### Request example

```json
{
  "refreshToken": "eyJ..."
}
```

### Success response — 200

The old refresh token is revoked. A new refresh token is issued through `Set-Cookie` and a new access token is returned in JSON.

```json
{
  "statusCode": 200,
  "data": {
    "accessToken": "eyJ...(new)",
    "refreshToken": "eyJ...(new)"
  },
  "message": "Token refreshed",
  "timestamp": "20xx-02-28T10:15:00"
}
```

Response header:

```http
Set-Cookie: refresh_token=<new-opaque-token>; HttpOnly; Secure; SameSite=Lax; Path=/api/v1/auth
```

### Failure response — 401

```json
{
  "statusCode": 401,
  "data": null,
  "message": "Refresh token is invalid, expired, revoked, or already used.",
  "timestamp": "2026-10-01T10:00:00"
}
```

### Rotation behavior

```text
R1
 ↓ refresh
R1 -> REVOKED
 ↓
create R2
create new Access Token
 ↓
R2 -> PostgreSQL
new Access Token jti -> Redis
```

A replay of an already-consumed refresh token must be rejected. The implementation should support refresh-token family tracking for replay detection.

---

## 3.3 Logout

```http
POST /api/v1/auth/logout
```

The access token is identified by its JWT `jti`. The corresponding access-token state is removed/revoked in Redis. The refresh token from the HttpOnly cookie is revoked in PostgreSQL. The refresh-token cookie is cleared.

### Success response — 200

```json
{
  "statusCode": 200,
  "data": null,
  "message": "Đăng xuất thành công",
  "timestamp": "2026-10-01T10:00:00"
}
```

Response header:

```http
Set-Cookie: refresh_token=; Max-Age=0; HttpOnly; Secure; SameSite=Lax; Path=/api/v1/auth
```

## 3.4 Customer password endpoints

Google-authenticated customers do not use application password endpoints:

```text
POST /api/v1/auth/forgot-password
POST /api/v1/auth/change-password
```

These endpoints are not part of the Customer authentication flow.

---

# 4. Public category & master data APIs

## 4.1 Get category tree

```http
GET /api/v1/categories/tree
```

Returns the hierarchical category tree. Only active, non-deleted categories are included.

### Success response — 200

```json
{
  "statusCode": 200,
  "data": [
    {
      "categoryId": 1,
      "name": "Tôn lợp mái",
      "status": "ACTIVE",
      "children": [
        {
          "categoryId": 10,
          "name": "Tôn sóng vuông",
          "status": "ACTIVE",
          "children": []
        }
      ]
    }
  ],
  "message": "Thành công",
  "timestamp": "2026-10-01T10:00:00"
}
```

## 4.2 Get products in category

```http
GET /api/v1/categories/{categoryId}/products
```

Only products with `ACTIVE` status are returned to public clients.

### Request example

```json
{}
```

### Success response — 200

```json
{
  "statusCode": 200,
  "data": {
    "categoryId": 10,
    "categoryName": "Tôn sóng",
    "products": [
      {
        "productId": 100,
        "name": "Tôn sóng A",
        "status": "ACTIVE"
      }
    ]
  },
  "message": "Thành công",
  "timestamp": "2026-10-01T10:00:00"
}
```

### Failure response — 404

```json
{
  "statusCode": 404,
  "data": null,
  "message": "Danh mục không tồn tại",
  "timestamp": "2026-10-01T10:00:00"
}
```

## 4.3 List active brands

```http
GET /api/v1/brands
```

Returns list of active brands for customer browsing and dropdown filters.

### Success response — 200

```json
{
  "statusCode": 200,
  "data": [
    {
      "brandId": 1,
      "brandName": "Hoa Sen",
      "status": "ACTIVE"
    },
    {
      "brandId": 2,
      "brandName": "Đông Á",
      "status": "ACTIVE"
    }
  ],
  "message": "Thành công",
  "timestamp": "2026-10-01T10:00:00"
}
```

## 4.4 List active units

```http
GET /api/v1/units
```

Returns list of active units (`m`, `tấm`, `cái`, etc.) for customer and manager reference.

### Success response — 200

```json
{
  "statusCode": 200,
  "data": [
    {
      "unitId": 1,
      "unitName": "m",
      "status": "ACTIVE"
    },
    {
      "unitId": 2,
      "unitName": "tấm",
      "status": "ACTIVE"
    }
  ],
  "message": "Thành công",
  "timestamp": "2026-10-01T10:00:00"
}
```

## 4.5 List active colors

```http
GET /api/v1/colors
```

Returns list of active manufacturing colors.

### Success response — 200

```json
{
  "statusCode": 200,
  "data": [
    {
      "colorId": 1,
      "colorName": "Xanh ngọc",
      "status": "ACTIVE"
    },
    {
      "colorId": 2,
      "colorName": "Đỏ đậm",
      "status": "ACTIVE"
    }
  ],
  "message": "Thành công",
  "timestamp": "2026-10-01T10:00:00"
}
```

---

# 5. Public product APIs

## 5.1 Search/filter products

```http
GET /api/v1/products
```

### Query parameters

```text
productName
categoryName
status
sortBy        # createdAt | price
sortDirection # ASC | DESC
priceUnit     # required when sorting by price and price sorting is allowed
page
size
```

Business requirements support filtering by product name, category, and product status, and sorting by time and price.

A caller without `PRODUCT_PRICE_VIEW` must not receive price values. Price sorting should not be exposed to such a caller because ordering can indirectly reveal price information.

### Request example

```json
{
  "productName": "Tôn",
  "categoryName": "Tôn sóng",
  "status": "ACTIVE",
  "sortBy": "createdAt",
  "sortDirection": "DESC",
  "page": 0,
  "size": 10
}
```

### Success response — 200

```json
{
  "statusCode": 200,
  "data": {
    "result": [
      {
        "productId": 100,
        "name": "Tôn sóng A",
        "description": "Mô tả sản phẩm.",
        "brand": {
          "brandId": 1,
          "name": "Brand A"
        },
        "unit": "m",
        "category": {
          "categoryId": 10,
          "name": "Tôn sóng"
        },
        "images": [],
        "variants": [],
        "prices": null,
        "priceVisible": false,
        "status": "ACTIVE"
      }
    ],
    "meta": {
      "page": 0,
      "size": 10,
      "totalElements": 1,
      "totalPages": 1
    }
  },
  "message": "Thành công",
  "timestamp": "2026-10-01T10:00:00"
}
```

### Success response when caller has `PRODUCT_PRICE_VIEW` — 200

```json
{
  "statusCode": 200,
  "data": {
    "result": [
      {
        "productId": 100,
        "name": "Tôn sóng A",
        "prices": {
          "m": 100000,
          "tam": 150000
        },
        "priceVisible": true,
        "status": "ACTIVE"
      }
    ],
    "meta": {
      "page": 0,
      "size": 10,
      "totalElements": 1,
      "totalPages": 1
    }
  },
  "message": "Thành công",
  "timestamp": "2026-10-01T10:00:00"
}
```

### Failure response — 400

```json
{
  "statusCode": 400,
  "data": null,
  "message": "Invalid search parameters.",
  "timestamp": "2026-10-01T10:00:00"
}
```

---

## 5.2 Product detail

```http
GET /api/v1/products/{productId}
```

### Request example

```json
{}
```

### Success response — 200

```json
{
  "statusCode": 200,
  "data": {
    "productId": 1,
    "name": "Tôn sóng",
    "description": "...",
    "brand": {
      "brandId": 1,
      "name": "Brand A"
    },
    "unit": "m",
    "category": {
      "categoryId": 10,
      "name": "Tôn sóng"
    },
    "images": [],
    "variants": [],
    "prices": null,
    "priceVisible": false,
    "status": "ACTIVE"
  },
  "message": "Thành công",
  "timestamp": "2026-10-01T10:00:00"
}
```

When the caller has `PRODUCT_PRICE_VIEW`, `prices` may be returned as a unit-to-price map:

```json
{
  "statusCode": 200,
  "data": {
    "productId": 1,
    "name": "Tôn sóng",
    "prices": {
      "m": 100000,
      "tam": 150000
    },
    "priceVisible": true,
    "status": "ACTIVE"
  },
  "message": "Thành công",
  "timestamp": "2026-10-01T10:00:00"
}
```

### Failure response — 404

```json
{
  "statusCode": 404,
  "data": null,
  "message": "Product not found.",
  "timestamp": "2026-10-01T10:00:00"
}
```

Price fields must not be leaked through search, category listing, detail, variant, or other public endpoints when the caller lacks `PRODUCT_PRICE_VIEW`.

---

# 6. Customer profile and address APIs

## 6.1 Get profile list 

only for manager, admin

```http
GET /api/v1/profiles

```

```json
{
  "statusCode": 200,
  "data": {
    "result": [
      {
        "userId": 1001,
        "fullName": "Nguyen Van A",
        "email": "user@example.com",
        "phone": "0987654321",
        "status": "ACTIVE"
      },
      {
        "userId": 1002,
        "fullName": "Nguyen Van B",
        "email": "userB@example.com",
        "phone": "0987654320",
        "status": "ACTIVE"
      }
    ],
    "meta": {
      "page": 0,
      "size": 10,
      "totalElements": 2,
      "totalPages": 1
    }
  },
  "message": "Thành công",
  "timestamp": "2026-10-01T10:00:00"
}
```

## 6.2 Get own profile

```http
GET /api/v1/me

```

### Request example

```json
{}
```

### Success response — 200

```json
{
  "statusCode": 200,
  "data": {
    "userId": 1001,
    "fullName": "Nguyen Van A",
    "email": "user@example.com",
    "phone": "0987654321",
    "status": "ACTIVE"
  },
  "message": "Thành công",
  "timestamp": "2026-10-01T10:00:00"
}
```

### Failure response — 401

```json
{
  "statusCode": 401,
  "data": null,
  "message": "Authentication is required.",
  "timestamp": "2026-10-01T10:00:00"
}
```

## 6.3 Update own profile

```http
PATCH /api/v1/me
```

### Request example

```json
{
  "fullName": "Nguyen Van B",
  "phone": "0987654321"
}
```

If the user enters a phone number for the first time or changes it, OTP verification is required according to the current requirement.

### Success response — 200

```json
{
  "statusCode": 200,
  "data": {
    "userId": 1001,
    "fullName": "Nguyen Van B",
    "email": "user@example.com",
    "phone": "0987654321"
  },
  "message": "Thành công",
  "timestamp": "2026-10-01T10:00:00"
}
```

### Failure response — 400

```json
{
  "statusCode": 400,
  "data": null,
  "message": "Validation failed.",
  "timestamp": "2026-10-01T10:00:00"
}
```

## 6.4 List saved addresses

```http
GET /api/v1/me/addresses
```

### Request example

```json
{}
```

### Success response — 200

```json
{
  "statusCode": 200,
  "data": [
    {
      "addressId": 1,
      "receiverName": "Nguyen Van A",
      "receiverPhone": "0987654321",
      "address": "123 Example Street",
      "isDefault": true,
      "status": "ACTIVE"
    }
  ],
  "message": "Thành công",
  "timestamp": "2026-10-01T10:00:00"
}
```

### Failure response — 401

```json
{
  "statusCode": 401,
  "data": null,
  "message": "Authentication is required.",
  "timestamp": "2026-10-01T10:00:00"
}
```

## 6.5 Create address

```http
POST /api/v1/me/addresses
```

### Request example

```json
{
  "receiverName": "Nguyen Van A",
  "receiverPhone": "0987654321",
  "address": "123 Example Street",
  "isDefault": true
}
```

### Success response — 201

```json
{
  "statusCode": 201,
  "data": {
    "addressId": 2,
    "receiverName": "Nguyen Van A",
    "receiverPhone": "0987654321",
    "address": "123 Example Street",
    "isDefault": true,
    "status": "ACTIVE"
  },
  "message": "Thành công",
  "timestamp": "2026-10-01T10:00:00"
}
```

### Failure response — 400

```json
{
  "statusCode": 400,
  "data": null,
  "message": "Validation failed.",
  "timestamp": "2026-10-01T10:00:00"
}
```

## 6.6 Update address

```http
PATCH /api/v1/me/addresses/{addressId}
```

### Request example

```json
{
  "receiverName": "Nguyen Van A",
  "receiverPhone": "0987654321",
  "address": "456 New Street",
  "isDefault": false
}
```

### Success response — 200

```json
{
  "statusCode": 200,
  "data": {
    "addressId": 2,
    "receiverName": "Nguyen Van A",
    "receiverPhone": "0987654321",
    "address": "456 New Street",
    "isDefault": false,
    "status": "ACTIVE"
  },
  "message": "Thành công",
  "timestamp": "2026-10-01T10:00:00"
}
```

### Failure response — 404

```json
{
  "statusCode": 404,
  "data": null,
  "message": "Address not found.",
  "timestamp": "2026-10-01T10:00:00"
}
```

## 6.7 Delete address

```http
DELETE /api/v1/me/addresses/{addressId}
```

Deletion is permanent and has no restore operation. Historical orders use an address snapshot and are not changed by address deletion.

### Request example

```json
{}
```

### Success response — 200

```json
{
  "statusCode": 200,
  "data": null,
  "message": "Thành công",
  "timestamp": "2026-10-01T10:00:00"
}
```

### Failure response — 409

```json
{
  "statusCode": 409,
  "data": null,
  "message": "Address cannot be deleted because it is required by the current account state.",
  "timestamp": "2026-10-01T10:00:00"
}
```

---

# 7. OTP APIs

## 7.1 Request OTP

```http
POST /api/v1/me/otp/request
```

### Request example

```json
{
  "phone": "0987654321"
}
```

OTP is stored temporarily in Redis with TTL and is never returned in the API response.

### Success response — 200

```json
{
  "statusCode": 200,
  "data": {
    "phone": "0987654321",
    "expiresIn": 120
  },
  "message": "OTP sent successfully.",
  "timestamp": "2026-10-01T10:00:00"
}
```

### Failure response — 429

```json
{
  "statusCode": 429,
  "data": null,
  "message": "Too many OTP requests. Please try again later.",
  "timestamp": "2026-10-01T10:00:00"
}
```

## 7.2 Verify OTP

```http
POST /api/v1/me/otp/verify
```

### Request example

```json
{
  "phone": "0987654321",
  "otp": "123456"
}
```

### Success response — 200

```json
{
  "statusCode": 200,
  "data": {
    "phone": "0987654321",
    "verified": true
  },
  "message": "OTP verified successfully.",
  "timestamp": "2026-10-01T10:00:00"
}
```

### Failure response — 400

```json
{
  "statusCode": 400,
  "data": null,
  "message": "Invalid OTP.",
  "timestamp": "2026-10-01T10:00:00"
}
```

Other possible failure cases:

```json
{
  "statusCode": 400,
  "data": null,
  "message": "OTP has expired.",
  "timestamp": "2026-10-01T10:00:00"
}
```

```json
{
  "statusCode": 409,
  "data": null,
  "message": "Phone number is already linked to another account.",
  "timestamp": "2026-10-01T10:00:00"
}
```

---

# 8. Customer order APIs

## 8.1 Create order

```http
POST /api/v1/orders
```

Customer must be authenticated.

The order is created and stored directly in PostgreSQL within an atomic transaction.

Initial status:

```text
PENDING
```

Retail item: product + quantity.

Manufacturing item in V1 (tôn sóng): type + color + size/length + quantity.

### Request example

```json
{
  "receiverName": "Nguyen Van A",
  "receiverPhone": "0987654321",
  "shippingAddress": "123 Example Street",
  "note": "Giao giờ hành chính",
  "phoneVerified": true,
  "items": [
    {
      "productId": 100,
      "variantId": 1001,
      "quantity": 10,
      "size": 5,
      "type": "Type A",
      "color": "Xanh",
      "unit": "m"
    }
  ]
}
```

The authenticated user's ID must be taken from the JWT subject/server-side authentication context, not trusted from a `userId` request field.

### Success response — 201

```json
{
  "statusCode": 201,
  "data": {
    "orderId": 10025,
    "status": "PENDING",
    "totalAmount": null,
    "items": [
      {
        "orderItemId": 1,
        "productId": 100,
        "quantity": 10,
        "size": 5,
        "unit": "m",
        "unitPrice": null,
        "totalPrice": null
      }
    ]
  },
  "message": "Thành công",
  "timestamp": "2026-10-01T10:00:00"
}
```

### Failure response — 400

```json
{
  "statusCode": 400,
  "data": {
    "errors": [
      {
        "field": "quantity",
        "message": "Quantity must be greater than 0."
      }
    ]
  },
  "message": "Validation failed.",
  "timestamp": "2026-10-01T10:00:00"
}
```

---

## 8.2 List own orders

```http
GET /api/v1/orders
```

Query parameters:

```text
dateFrom
dateTo
status
```

### Request example

```json
{
  "dateFrom": "2026-09-01",
  "dateTo": "2026-10-01",
  "status": "PENDING",
  "page": 0,
  "size": 10
}
```

### Success response — 200

```json
{
  "statusCode": 200,
  "data": {
    "content": [
      {
        "orderId": 10025,
        "status": "PENDING",
        "totalAmount": null,
        "createdAt": "2026-10-01T09:30:00"
      }
    ],
    "meta": {
      "page": 0,
      "size": 10,
      "totalElements": 1,
      "totalPages": 1
    }
  },
  "message": "Thành công",
  "timestamp": "2026-10-01T10:00:00"
}
```

### Failure response — 401

```json
{
  "statusCode": 401,
  "data": null,
  "message": "Authentication is required.",
  "timestamp": "2026-10-01T10:00:00"
}
```

---

## 8.3 Get own order

```http
GET /api/v1/orders/{orderId}
```

The customer can see `TotalAmount` after Manager has calculated the order.

### Request example

```json
{}
```

### Success response — 200

```json
{
  "statusCode": 200,
  "data": {
    "orderId": 10025,
    "status": "WAITING_FOR_CUSTOMER_CONFIRMATION",
    "totalAmount": 5000000,
    "items": [
      {
        "orderItemId": 1,
        "productId": 100,
        "quantity": 10,
        "size": 5,
        "unit": "m",
        "unitPrice": 100000,
        "totalPrice": 5000000
      }
    ]
  },
  "message": "Thành công",
  "timestamp": "2026-10-01T10:00:00"
}
```

### Failure response — 404

```json
{
  "statusCode": 404,
  "data": null,
  "message": "Order not found.",
  "timestamp": "2026-10-01T10:00:00"
}
```

---

## 8.4 Confirm calculated order

```http
POST /api/v1/orders/{orderId}/confirm
```

Preconditions:

```text
caller owns order
AND
order has calculated total
AND
order is waiting for customer confirmation
```

### Request example

```json
{}
```

### Success response — 200

```json
{
  "statusCode": 200,
  "data": {
    "orderId": 10025,
    "status": "CONFIRMED",
    "totalAmount": 5000000
  },
  "message": "Thành công",
  "timestamp": "2026-10-01T10:00:00"
}
```

### Failure response — 409

```json
{
  "statusCode": 409,
  "data": null,
  "message": "Order cannot be confirmed in its current state.",
  "timestamp": "2026-10-01T10:00:00"
}
```

---

## 8.5 Cancel own order

```http
POST /api/v1/orders/{orderId}/cancel
```

Cancellation reason is optional for all roles.

### Request example

```json
{
  "reason": "Không còn nhu cầu"
}
```

The request can also be:

```json
{}
```

### Success response — 200

```json
{
  "statusCode": 200,
  "data": {
    "orderId": 10025,
    "status": "CANCELLED",
    "reason": null
  },
  "message": "Thành công",
  "timestamp": "2026-10-01T10:00:00"
}
```

### Failure response — 409

```json
{
  "statusCode": 409,
  "data": null,
  "message": "Order cannot be cancelled at this time.",
  "timestamp": "2026-10-01T10:00:00"
}
```

The server validates both the configured cancellation window and the current order status.

---

# 9. Manager category APIs

Categories form a tree similar to a file-system folder structure.

```text
Category Root
├── Child Category A
│   ├── Child Category A1
│   └── Product A
└── Product B
```

A Manager can create either a child category or a product under the current category.

## 9.1 Create root category

```http
POST /api/v1/manager/categories
```

### Request example

```json
{
  "name": "Vật liệu xây dựng"
}
```

### Success response — 201

```json
{
  "statusCode": 201,
  "data": {
    "categoryId": 1,
    "name": "Vật liệu xây dựng",
    "parentCategoryId": null,
    "status": "ACTIVE"
  },
  "message": "Thành công",
  "timestamp": "2026-10-01T10:00:00"
}
```

### Failure response — 409

```json
{
  "statusCode": 409,
  "data": null,
  "message": "Category name already exists at this level.",
  "timestamp": "2026-10-01T10:00:00"
}
```

## 9.2 Create child category

```http
POST /api/v1/manager/categories/{parentId}/children
```

### Request example

```json
{
  "name": "Tôn sóng"
}
```

### Success response — 201

```json
{
  "statusCode": 201,
  "data": {
    "categoryId": 10,
    "name": "Tôn sóng",
    "parentCategoryId": 1,
    "status": "ACTIVE"
  },
  "message": "Thành công",
  "timestamp": "2026-10-01T10:00:00"
}
```

### Failure response — 404

```json
{
  "statusCode": 404,
  "data": null,
  "message": "Parent category not found.",
  "timestamp": "2026-10-01T10:00:00"
}
```

## 9.3 Update category

```http
PATCH /api/v1/manager/categories/{categoryId}
```

### Request example

```json
{
  "name": "Tôn lợp mái"
}
```

### Success response — 200

```json
{
  "statusCode": 200,
  "data": {
    "categoryId": 10,
    "name": "Tôn lợp mái",
    "parentCategoryId": 1,
    "status": "ACTIVE"
  },
  "message": "Thành công",
  "timestamp": "2026-10-01T10:00:00"
}
```

### Failure response — 404

```json
{
  "statusCode": 404,
  "data": null,
  "message": "Category not found.",
  "timestamp": "2026-10-01T10:00:00"
}
```

## 9.4 Lock category

```http
POST /api/v1/manager/categories/{categoryId}/lock
```

### Request example

```json
{}
```

### Success response — 200

```json
{
  "statusCode": 200,
  "data": {
    "categoryId": 10,
    "status": "LOCKED"
  },
  "message": "Thành công",
  "timestamp": "2026-10-01T10:00:00"
}
```

### Failure response — 409

```json
{
  "statusCode": 409,
  "data": null,
  "message": "Category cannot be locked in its current state.",
  "timestamp": "2026-10-01T10:00:00"
}
```

## 9.5 Unlock category

```http
POST /api/v1/manager/categories/{categoryId}/unlock
```

### Request example

```json
{}
```

### Success response — 200

```json
{
  "statusCode": 200,
  "data": {
    "categoryId": 10,
    "status": "ACTIVE"
  },
  "message": "Thành công",
  "timestamp": "2026-10-01T10:00:00"
}
```

### Failure response — 409

```json
{
  "statusCode": 409,
  "data": null,
  "message": "Category cannot be unlocked in its current state.",
  "timestamp": "2026-10-01T10:00:00"
}
```

## 9.6 Delete category

```http
DELETE /api/v1/manager/categories/{categoryId}
DELETE /api/v1/manager/categories/{categoryId}?force=true
```

Performs a soft delete (`isDeleted = true`).

Business rules:
1. **If the category is empty** (contains no child categories and no products): Soft-deleted immediately.
2. **If the category contains child categories or products**:
   - Calling `DELETE /api/v1/manager/categories/{categoryId}` (without `force=true`):
     The server rejects deletion with `409 Conflict`, returning the counts of impacted child categories and products so the frontend client can display a confirmation popup.
   - When the user confirms in the popup and the client calls `DELETE /api/v1/manager/categories/{categoryId}?force=true`:
     - The server validates whether any product in the sub-tree belongs to an **incomplete order** (status other than `COMPLETED` or `CANCELLED`). If so, deletion is rejected with `409 Conflict`.
     - Otherwise, the server performs a cascade soft delete: marks `isDeleted = true` for the current category and all child categories and products under its tree.

### Success response — 200

```json
{
  "statusCode": 200,
  "data": null,
  "message": "Xóa danh mục thành công.",
  "timestamp": "2026-10-01T10:00:00"
}
```

### Failure response when confirmation is required (without `force=true`) — 409

```json
{
  "statusCode": 409,
  "data": {
    "childCategoryCount": 5,
    "productCount": 15,
    "totalImpactCount": 20,
    "requiresConfirmation": true
  },
  "message": "Trong danh mục này chứa 20 danh mục con và sản phẩm khác. Bạn có chắc chắn muốn xóa danh mục này không?",
  "timestamp": "2026-10-01T10:00:00"
}
```

### Failure response when sub-tree contains a product in an incomplete order — 409

```json
{
  "statusCode": 409,
  "data": {
    "productId": 100,
    "orderId": 10025,
    "orderStatus": "PROCESSING"
  },
  "message": "Không thể xóa vì sản phẩm 'Tôn sóng A' đang nằm trong đơn hàng #10025 chưa hoàn thành.",
  "timestamp": "2026-10-01T10:00:00"
}
```

## 9.6 Get products by category (Manager & Admin)

```http
GET /api/v1/manager/categories/{categoryId}/products
```

Returns all non-deleted products under the given category, including both `ACTIVE` and `INACTIVE` products, for operational and inventory management by Managers and Admins.

Requires permission `PRODUCT_CATEGORY_VIEW` (granted to `ADMIN` and `MANAGER`).

### Success response — 200

```json
{
  "statusCode": 200,
  "data": {
    "categoryId": 1,
    "categoryName": "Tôn lợp mái",
    "products": [
      {
        "id": 1,
        "productName": "Tôn Hoa Sen 11 sóng vuông",
        "status": "ACTIVE",
        "categoryId": 1,
        "categoryName": "Tôn lợp mái",
        "brandId": 1,
        "brandName": "Tôn Hoa Sen",
        "primaryImageUrl": "https://cdn.example.com/products/ton-hoa-sen-1.jpg",
        "priceVisible": true
      },
      {
        "id": 2,
        "productName": "Tôn sóng cũ ngừng sản xuất",
        "status": "INACTIVE",
        "categoryId": 1,
        "categoryName": "Tôn lợp mái",
        "brandId": 1,
        "brandName": "Tôn Hoa Sen",
        "primaryImageUrl": null,
        "priceVisible": true
      }
    ]
  },
  "message": "Thành công",
  "timestamp": "2026-10-28T20:00:00"
}
```

### Failure response — 404

```json
{
  "statusCode": 404,
  "data": null,
  "message": "Không tìm thấy danh mục ID: 99",
  "timestamp": "2026-10-28T20:00:00"
}
```

---

# 10. Manager product APIs

## 10.1 Create product under category

```http
POST /api/v1/manager/categories/{categoryId}/products
```

A Manager chooses a category folder and then creates the product under that category.

Price is represented as a map:

```text
key   = unit
value = price
```

Example:

```json
{
  "m": 100000,
  "tam": 150000,
  "kg": 75000
}
```

This allows the same product to have different prices by selling unit.

### Request example

```json
{
  "name": "Tôn sóng A",
  "description": "Mô tả sản phẩm.",
  "brandId": 1,
  "size": "1m x 5m",
  "prices": {
    "m": 100000,
    "tam": 150000
  },
  "variants": [
    {
      "type": "Type A",
      "thickness": "0.40mm",
      "color": "Xanh"
    }
  ]
}
```

### Success response — 201

```json
{
  "statusCode": 201,
  "data": {
    "productId": 100,
    "categoryId": 10,
    "name": "Tôn sóng A",
    "prices": {
      "m": 100000,
      "tam": 150000
    },
    "status": "ACTIVE"
  },
  "message": "Thành công",
  "timestamp": "2026-10-01T10:00:00"
}
```

### Failure response — 400

```json
{
  "statusCode": 400,
  "data": null,
  "message": "Product validation failed.",
  "timestamp": "2026-10-01T10:00:00"
}
```

## 10.2 Update product

```http
PATCH /api/v1/manager/products/{productId}
```

### Request example

```json
{
  "name": "Tôn sóng A cập nhật",
  "description": "Mô tả mới.",
  "prices": {
    "m": 105000,
    "tam": 155000
  }
}
```

### Success response — 200

```json
{
  "statusCode": 200,
  "data": {
    "productId": 100,
    "name": "Tôn sóng A cập nhật",
    "prices": {
      "m": 105000,
      "tam": 155000
    },
    "status": "ACTIVE"
  },
  "message": "Thành công",
  "timestamp": "2026-10-01T10:00:00"
}
```

### Failure response — 404

```json
{
  "statusCode": 404,
  "data": null,
  "message": "Product not found.",
  "timestamp": "2026-10-01T10:00:00"
}
```

## 10.3 Lock product

```http
POST /api/v1/manager/products/{productId}/lock
```

### Request example

```json
{}
```

### Success response — 200

```json
{
  "statusCode": 200,
  "data": {
    "productId": 100,
    "status": "LOCKED"
  },
  "message": "Thành công",
  "timestamp": "2026-10-01T10:00:00"
}
```

### Failure response — 409

```json
{
  "statusCode": 409,
  "data": null,
  "message": "Product cannot be locked in its current state.",
  "timestamp": "2026-10-01T10:00:00"
}
```

## 10.4 Unlock product

```http
POST /api/v1/manager/products/{productId}/unlock
```

### Request example

```json
{}
```

### Success response — 200

```json
{
  "statusCode": 200,
  "data": {
    "productId": 100,
    "status": "ACTIVE"
  },
  "message": "Thành công",
  "timestamp": "2026-10-01T10:00:00"
}
```

### Failure response — 409

```json
{
  "statusCode": 409,
  "data": null,
  "message": "Product cannot be unlocked in its current state.",
  "timestamp": "2026-10-01T10:00:00"
}
```

## 10.5 Delete product

```http
DELETE /api/v1/manager/products/{productId}
```

Performs a soft delete (`isDeleted = true`).

Business rules:
- Soft delete is supported even if the product was referenced in completed (`COMPLETED`) or cancelled (`CANCELLED`) historical orders, as historical orders retain immutable snapshots.
- Soft deleting a product automatically marks its associated variants (`ProductVariant`) and images (`ProductImage`) as `isDeleted = true`.
- **Deletion is REJECTED (409 Conflict)** ONLY WHEN this product belongs to an active, incomplete order (status: `PENDING`, `CONFIRMED`, `PROCESSING`, `SHIPPING`).

### Success response — 200

```json
{
  "statusCode": 200,
  "data": null,
  "message": "Xóa sản phẩm thành công.",
  "timestamp": "2026-10-01T10:00:00"
}
```

### Failure response when product is in an incomplete order — 409

```json
{
  "statusCode": 409,
  "data": {
    "productId": 100,
    "orderId": 10025,
    "orderStatus": "PROCESSING"
  },
  "message": "Không thể xóa sản phẩm vì đang nằm trong đơn hàng #10025 chưa hoàn thành.",
  "timestamp": "2026-10-01T10:00:00"
}
```

## 10.6 Add product image

```http
POST /api/v1/manager/products/{productId}/images
```

The request body may use multipart/form-data because an image file is uploaded. The API response is still JSON.

The product supports one main image and up to ten secondary images according to the current requirement.

### Request example

```json
{
  "imageType": "PRIMARY",
  "fileName": "ton-song-a.jpg"
}
```

### Success response — 201

```json
{
  "statusCode": 201,
  "data": {
    "imageId": 501,
    "productId": 100,
    "imageType": "PRIMARY",
    "url": "https://example.com/products/100/ton-song-a.jpg"
  },
  "message": "Thành công",
  "timestamp": "2026-10-01T10:00:00"
}
```

### Failure response — 400

```json
{
  "statusCode": 400,
  "data": null,
  "message": "Invalid image upload.",
  "timestamp": "2026-10-01T10:00:00"
}
```

## 10.7 Delete product image

```http
DELETE /api/v1/manager/products/{productId}/images/{imageId}
```

Deletion is permanent.

### Request example

```json
{}
```

### Success response — 200

```json
{
  "statusCode": 200,
  "data": null,
  "message": "Thành công",
  "timestamp": "2026-10-01T10:00:00"
}
```

### Failure response — 404

```json
{
  "statusCode": 404,
  "data": null,
  "message": "Product image not found.",
  "timestamp": "2026-10-01T10:00:00"
}
```

## 10.8 Manage product variants

```http
POST  /api/v1/manager/products/{productId}/variants
PATCH /api/v1/manager/products/{productId}/variants/{variantId}
POST  /api/v1/manager/products/{productId}/variants/{variantId}/lock
POST  /api/v1/manager/products/{productId}/variants/{variantId}/unlock
DELETE /api/v1/manager/products/{productId}/variants/{variantId}
```

### Create variant request example

```json
{
  "type": "Type A",
  "thickness": "0.40mm",
  "color": "Xanh"
}
```

### Success response — 201

```json
{
  "statusCode": 201,
  "data": {
    "variantId": 1001,
    "productId": 100,
    "type": "Type A",
    "thickness": "0.40mm",
    "color": "Xanh",
    "status": "ACTIVE"
  },
  "message": "Thành công",
  "timestamp": "2026-10-01T10:00:00"
}
```

### Failure response — 409

```json
{
  "statusCode": 409,
  "data": null,
  "message": "This product variant already exists.",
  "timestamp": "2026-10-01T10:00:00"
}
```

### Delete variant success — 200

```json
{
  "statusCode": 200,
  "data": null,
  "message": "Thành công",
  "timestamp": "2026-10-01T10:00:00"
}
```

### Delete variant failure — 409

```json
{
  "statusCode": 409,
  "data": null,
  "message": "Variant cannot be deleted because it is referenced by historical orders.",
  "timestamp": "2026-10-01T10:00:00"
}
```

Locking a variant is reversible; deleting it is permanent.

---

# 11. Manager order APIs

## 11.1 List all orders

```http
GET /api/v1/manager/orders
```

Filters required by the current requirements:

```text
dateFrom
dateTo
customerName
customerPhone
customerEmail
status
```

### Request example

```json
{
  "dateFrom": "2026-09-01",
  "dateTo": "2026-10-01",
  "customerName": "Nguyen Van A",
  "status": "PENDING",
  "page": 0,
  "size": 20
}
```

### Success response — 200

```json
{
  "statusCode": 200,
  "data": {
    "content": [
      {
        "orderId": 10025,
        "customer": {
          "userId": 1001,
          "fullName": "Nguyen Van A",
          "phone": "0987654321"
        },
        "status": "PENDING",
        "totalAmount": null,
        "createdAt": "2026-10-01T09:30:00"
      }
    ],
    "meta": {
      "page": 0,
      "size": 10,
      "totalElements": 1,
      "totalPages": 1
    }
  },
  "message": "Thành công",
  "timestamp": "2026-10-01T10:00:00"
}
```

### Failure response — 403

```json
{
  "statusCode": 403,
  "data": null,
  "message": "You are not authorized to view all orders.",
  "timestamp": "2026-10-01T10:00:00"
}
```

## 11.2 Get order detail

```http
GET /api/v1/manager/orders/{orderId}
```

### Request example

```json
{}
```

### Success response — 200

```json
{
  "statusCode": 200,
  "data": {
    "orderId": 10025,
    "customer": {
      "userId": 1001,
      "fullName": "Nguyen Van A",
      "phone": "0987654321"
    },
    "status": "PENDING",
    "totalAmount": null,
    "items": []
  },
  "message": "Thành công",
  "timestamp": "2026-10-01T10:00:00"
}
```

### Failure response — 404

```json
{
  "statusCode": 404,
  "data": null,
  "message": "Order not found.",
  "timestamp": "2026-10-01T10:00:00"
}
```

## 11.3 Calculate order price

```http
POST /api/v1/manager/orders/{orderId}/calculate
```

For manufacturing items in V1:

```text
TotalPrice = UnitPrice × Size × Quantity
```

The unit price is selected from the product's price map using the order item's selected unit.

Example price map:

```json
{
  "m": 100000,
  "tam": 150000
}
```

### Request example

```json
{
  "unit": "m"
}
```

### Success response — 200

```json
{
  "statusCode": 200,
  "data": {
    "orderId": 10025,
    "status": "WAITING_FOR_CUSTOMER_CONFIRMATION",
    "totalAmount": 5000000,
    "items": [
      {
        "orderItemId": 1,
        "unit": "m",
        "unitPrice": 100000,
        "size": 5,
        "quantity": 10,
        "totalPrice": 5000000
      }
    ]
  },
  "message": "Thành công",
  "timestamp": "2026-10-01T10:00:00"
}
```

### Failure response — 409

```json
{
  "statusCode": 409,
  "data": null,
  "message": "Order cannot be calculated in its current state.",
  "timestamp": "2026-10-01T10:00:00"
}
```

After calculation, the Manager contacts the customer to communicate the calculated price. The customer then sees the total amount and can confirm the order.

## 11.4 Cancel order

```http
POST /api/v1/manager/orders/{orderId}/cancel
```

Cancellation reason is optional.

### Request example

```json
{
  "reason": "Sản phẩm không đáp ứng yêu cầu"
}
```

### Success response — 200

```json
{
  "statusCode": 200,
  "data": {
    "orderId": 10025,
    "status": "CANCELLED",
    "reason": "Sản phẩm không đáp ứng yêu cầu"
  },
  "message": "Thành công",
  "timestamp": "2026-10-01T10:00:00"
}
```

### Failure response — 409

```json
{
  "statusCode": 409,
  "data": null,
  "message": "Order cannot be cancelled in its current state.",
  "timestamp": "2026-10-01T10:00:00"
}
```

## 11.5 Update order status

```http
PATCH /api/v1/manager/orders/{orderId}/status
```

### Request example

```json
{
  "status": "PROCESSING",
  "reason": "Đã bắt đầu chuẩn bị hàng"
}
```

`reason` here may be optional unless a later business rule makes it mandatory for a specific transition.

### Success response — 200

```json
{
  "statusCode": 200,
  "data": {
    "orderId": 10025,
    "status": "PROCESSING"
  },
  "message": "Thành công",
  "timestamp": "2026-10-01T10:00:00"
}
```

### Failure response — 409

```json
{
  "statusCode": 409,
  "data": null,
  "message": "Invalid order status transition.",
  "timestamp": "2026-10-01T10:00:00"
}
```

Every successful status transition must append an `OrderStatusHistory` record.

---

# 12. Admin user APIs

## 12.1 List users

```http
GET /api/v1/admin/users
```

Filters:

```text
name
phone
email
status
```

### Request example

```json
{
  "name": "Nguyen",
  "status": "ACTIVE",
  "page": 0,
  "size": 20
}
```

### Success response — 200

```json
{
  "statusCode": 200,
  "data": {
    "content": [
      {
        "userId": 1001,
        "fullName": "Nguyen Van A",
        "email": "user@example.com",
        "phone": "0987654321",
        "roles": ["CUSTOMER"],
        "status": "ACTIVE"
      }
    ],
    "meta": {
      "page": 0,
      "size": 10,
      "totalElements": 1,
      "totalPages": 1
    }
  },
  "message": "Thành công",
  "timestamp": "2026-10-01T10:00:00"
}
```

### Failure response — 403

```json
{
  "statusCode": 403,
  "data": null,
  "message": "You are not authorized to manage users.",
  "timestamp": "2026-10-01T10:00:00"
}
```

## 12.2 Lock user

```http
POST /api/v1/admin/users/{userId}/lock
```

### Request example

```json
{}
```

### Success response — 200

```json
{
  "statusCode": 200,
  "data": {
    "userId": 1001,
    "status": "LOCKED"
  },
  "message": "Thành công",
  "timestamp": "2026-10-01T10:00:00"
}
```

### Failure response — 409

```json
{
  "statusCode": 409,
  "data": null,
  "message": "User is already locked or cannot be locked.",
  "timestamp": "2026-10-01T10:00:00"
}
```

## 12.3 Unlock user

```http
POST /api/v1/admin/users/{userId}/unlock
```

### Request example

```json
{}
```

### Success response — 200

```json
{
  "statusCode": 200,
  "data": {
    "userId": 1001,
    "status": "ACTIVE"
  },
  "message": "Thành công",
  "timestamp": "2026-10-01T10:00:00"
}
```

### Failure response — 409

```json
{
  "statusCode": 409,
  "data": null,
  "message": "User is already active or cannot be unlocked.",
  "timestamp": "2026-10-01T10:00:00"
}
```

## 12.4 Delete user

```http
DELETE /api/v1/admin/users/{userId}
```

Deletion is permanent and cannot be restored.

Because orders and audit records may reference users, the implementation must reject destructive deletion when the database integrity/business-history rules would be violated.

### Request example

```json
{}
```

### Success response — 200

```json
{
  "statusCode": 200,
  "data": null,
  "message": "Thành công",
  "timestamp": "2026-10-01T10:00:00"
}
```

### Failure response — 409

```json
{
  "statusCode": 409,
  "data": null,
  "message": "User cannot be permanently deleted because historical records depend on this account.",
  "timestamp": "2026-10-01T10:00:00"
}
```

---

# 13. Admin Manager-account APIs

## 13.1 Create Manager account

```http
POST /api/v1/admin/managers
```

Admin provisions a Manager account by Google identity. No local password is created for the Manager.

The expected business flow is:

```text
Admin enters Manager Google email / identity
    ↓
Backend creates or provisions Manager account
    ↓
Manager later authenticates through Google
    ↓
Backend recognizes the provisioned Google identity
    ↓
Manager receives application JWT access token + refresh token
```

The exact Google account-linking payload remains `TBD` until the frontend/admin onboarding flow is finalized.

### Request example

```json
{
  "email": "manager@example.com",
  "fullName": "Manager A"
}
```

### Success response — 201

```json
{
  "statusCode": 201,
  "data": {
    "userId": 2001,
    "fullName": "Manager A",
    "email": "manager@example.com",
    "roles": ["MANAGER"],
    "authProvider": "GOOGLE",
    "status": "ACTIVE"
  },
  "message": "Thành công",
  "timestamp": "2026-10-01T10:00:00"
}
```

### Failure response — 409

```json
{
  "statusCode": 409,
  "data": null,
  "message": "A user already exists for this Google email.",
  "timestamp": "2026-10-01T10:00:00"
}
```

---

# 14. Admin permission APIs

## 14.1 View role permissions

```http
GET /api/v1/admin/roles/{roleId}/permissions
```

### Request example

```json
{}
```

### Success response — 200

```json
{
  "statusCode": 200,
  "data": {
    "roleId": 3,
    "roleName": "CUSTOMER",
    "permissions": [
      "PRODUCT_VIEW",
      "ORDER_CREATE",
      "ORDER_VIEW_OWN"
    ]
  },
  "message": "Thành công",
  "timestamp": "2026-10-01T10:00:00"
}
```

### Failure response — 404

```json
{
  "statusCode": 404,
  "data": null,
  "message": "Role not found.",
  "timestamp": "2026-10-01T10:00:00"
}
```

## 14.2 Replace role permissions

```http
PUT /api/v1/admin/roles/{roleId}/permissions
```

### Request example

```json
{
  "permissions": [
    "PRODUCT_VIEW",
    "PRODUCT_PRICE_VIEW"
  ]
}
```

### Success response — 200

```json
{
  "statusCode": 200,
  "data": {
    "roleId": 3,
    "roleName": "CUSTOMER",
    "permissions": [
      "PRODUCT_VIEW",
      "PRODUCT_PRICE_VIEW"
    ]
  },
  "message": "Thành công",
  "timestamp": "2026-10-01T10:00:00"
}
```

### Failure response — 400

```json
{
  "statusCode": 400,
  "data": null,
  "message": "One or more permissions are invalid.",
  "timestamp": "2026-10-01T10:00:00"
}
```

This is the mechanism used by Admin to enable Customer price visibility without changing business code.

---

# 15. Admin system-setting APIs

## 15.1 List settings

```http
GET /api/v1/admin/settings
```

### Request example

```json
{}
```

### Success response — 200

```json
{
  "statusCode": 200,
  "data": [
    {
      "key": "ORDER_CUSTOMER_CANCELLATION_WINDOW_MINUTES",
      "value": "30",
      "dataType": "INTEGER"
    }
  ],
  "message": "Thành công",
  "timestamp": "2026-10-01T10:00:00"
}
```

### Failure response — 403

```json
{
  "statusCode": 403,
  "data": null,
  "message": "You are not authorized to view system settings.",
  "timestamp": "2026-10-01T10:00:00"
}
```

## 15.2 Update setting

```http
PUT /api/v1/admin/settings/{key}
```

### Request example

```json
{
  "value": "60"
}
```

### Success response — 200

```json
{
  "statusCode": 200,
  "data": {
    "key": "ORDER_CUSTOMER_CANCELLATION_WINDOW_MINUTES",
    "value": "60",
    "dataType": "INTEGER"
  },
  "message": "Thành công",
  "timestamp": "2026-10-01T10:00:00"
}
```

### Failure response — 400

```json
{
  "statusCode": 400,
  "data": null,
  "message": "Invalid setting value.",
  "timestamp": "2026-10-01T10:00:00"
}
```

All changes must be audited.

---

# 15.3 Admin brand management APIs

## 15.3.1 List brands

```http
GET /api/v1/admin/brands
```

Query parameters:
- `name`: search by brand name
- `status`: `ACTIVE` | `LOCKED`
- `page`: default 0
- `size`: default 10

### Success response — 200

```json
{
  "statusCode": 200,
  "data": {
    "result": [
      {
        "brandId": 1,
        "brandName": "Hoa Sen",
        "status": "ACTIVE",
        "createdAt": "2026-10-01T10:00:00",
        "updatedAt": "2026-10-01T10:00:00"
      }
    ],
    "meta": {
      "page": 0,
      "size": 10,
      "totalElements": 1,
      "totalPages": 1
    }
  },
  "message": "Thành công",
  "timestamp": "2026-10-01T10:00:00"
}
```

## 15.3.2 Create brand

```http
POST /api/v1/admin/brands
```

### Request example

```json
{
  "brandName": "Đông Á"
}
```

### Success response — 201

```json
{
  "statusCode": 201,
  "data": {
    "brandId": 2,
    "brandName": "Đông Á",
    "status": "ACTIVE"
  },
  "message": "Tạo thương hiệu thành công.",
  "timestamp": "2026-10-01T10:00:00"
}
```

### Failure response — 409

```json
{
  "statusCode": 409,
  "data": null,
  "message": "Tên thương hiệu đã tồn tại.",
  "timestamp": "2026-10-01T10:00:00"
}
```

## 15.3.3 Update brand

```http
PATCH /api/v1/admin/brands/{brandId}
```

### Request example

```json
{
  "brandName": "Tôn Đông Á (cập nhật)"
}
```

### Success response — 200

```json
{
  "statusCode": 200,
  "data": {
    "brandId": 2,
    "brandName": "Tôn Đông Á (cập nhật)",
    "status": "ACTIVE"
  },
  "message": "Cập nhật thương hiệu thành công.",
  "timestamp": "2026-10-01T10:00:00"
}
```

## 15.3.4 Lock / Unlock brand

```http
POST /api/v1/admin/brands/{brandId}/lock
POST /api/v1/admin/brands/{brandId}/unlock
```

### Success response — 200

```json
{
  "statusCode": 200,
  "data": {
    "brandId": 2,
    "status": "LOCKED"
  },
  "message": "Khóa thương hiệu thành công.",
  "timestamp": "2026-10-01T10:00:00"
}
```

## 15.3.5 Delete brand

```http
DELETE /api/v1/admin/brands/{brandId}
```

### Success response — 200

```json
{
  "statusCode": 200,
  "data": null,
  "message": "Xóa thương hiệu thành công.",
  "timestamp": "2026-10-01T10:00:00"
}
```

### Failure response — 409

```json
{
  "statusCode": 409,
  "data": null,
  "message": "Không thể xóa thương hiệu vì đang được sử dụng bởi các sản phẩm hiện có.",
  "timestamp": "2026-10-01T10:00:00"
}
```

---

# 15.4 Admin unit management APIs

## 15.4.1 List units

```http
GET /api/v1/admin/units
```

### Success response — 200

```json
{
  "statusCode": 200,
  "data": {
    "result": [
      {
        "unitId": 1,
        "unitName": "m",
        "status": "ACTIVE",
        "createdAt": "2026-10-01T10:00:00",
        "updatedAt": "2026-10-01T10:00:00"
      }
    ],
    "meta": {
      "page": 0,
      "size": 10,
      "totalElements": 1,
      "totalPages": 1
    }
  },
  "message": "Thành công",
  "timestamp": "2026-10-01T10:00:00"
}
```

## 15.4.2 Create unit

```http
POST /api/v1/admin/units
```

### Request example

```json
{
  "unitName": "cuộn"
}
```

### Success response — 201

```json
{
  "statusCode": 201,
  "data": {
    "unitId": 3,
    "unitName": "cuộn",
    "status": "ACTIVE"
  },
  "message": "Tạo đơn vị tính thành công.",
  "timestamp": "2026-10-01T10:00:00"
}
```

### Failure response — 409

```json
{
  "statusCode": 409,
  "data": null,
  "message": "Đơn vị tính đã tồn tại.",
  "timestamp": "2026-10-01T10:00:00"
}
```

## 15.4.3 Update unit

```http
PATCH /api/v1/admin/units/{unitId}
```

### Success response — 200

```json
{
  "statusCode": 200,
  "data": {
    "unitId": 3,
    "unitName": "cuộn lớn",
    "status": "ACTIVE"
  },
  "message": "Cập nhật đơn vị tính thành công.",
  "timestamp": "2026-10-01T10:00:00"
}
```

## 15.4.4 Lock / Unlock unit

```http
POST /api/v1/admin/units/{unitId}/lock
POST /api/v1/admin/units/{unitId}/unlock
```

## 15.4.5 Delete unit

```http
DELETE /api/v1/admin/units/{unitId}
```

### Failure response — 409

```json
{
  "statusCode": 409,
  "data": null,
  "message": "Không thể xóa đơn vị tính vì đang được sử dụng trong bảng giá sản phẩm.",
  "timestamp": "2026-10-01T10:00:00"
}
```

---

# 15.5 Admin color management APIs

## 15.5.1 List colors

```http
GET /api/v1/admin/colors
```

### Success response — 200

```json
{
  "statusCode": 200,
  "data": {
    "result": [
      {
        "colorId": 1,
        "colorName": "Xanh ngọc",
        "status": "ACTIVE",
        "createdAt": "2026-10-01T10:00:00",
        "updatedAt": "2026-10-01T10:00:00"
      }
    ],
    "meta": {
      "page": 0,
      "size": 10,
      "totalElements": 1,
      "totalPages": 1
    }
  },
  "message": "Thành công",
  "timestamp": "2026-10-01T10:00:00"
}
```

## 15.5.2 Create color

```http
POST /api/v1/admin/colors
```

### Request example

```json
{
  "colorName": "Xanh rêu"
}
```

### Success response — 201

```json
{
  "statusCode": 201,
  "data": {
    "colorId": 3,
    "colorName": "Xanh rêu",
    "status": "ACTIVE"
  },
  "message": "Tạo màu sắc thành công.",
  "timestamp": "2026-10-01T10:00:00"
}
```

### Failure response — 409

```json
{
  "statusCode": 409,
  "data": null,
  "message": "Màu sắc này đã tồn tại.",
  "timestamp": "2026-10-01T10:00:00"
}
```

## 15.5.3 Update color

```http
PATCH /api/v1/admin/colors/{colorId}
```

### Success response — 200

```json
{
  "statusCode": 200,
  "data": {
    "colorId": 3,
    "colorName": "Xanh rêu nhạt",
    "status": "ACTIVE"
  },
  "message": "Cập nhật màu sắc thành công.",
  "timestamp": "2026-10-01T10:00:00"
}
```

## 15.5.4 Lock / Unlock color

```http
POST /api/v1/admin/colors/{colorId}/lock
POST /api/v1/admin/colors/{colorId}/unlock
```

## 15.5.5 Delete color

```http
DELETE /api/v1/admin/colors/{colorId}
```

### Failure response — 409

```json
{
  "statusCode": 409,
  "data": null,
  "message": "Không thể xóa màu sắc vì đang được liên kết với các biến thể sản phẩm.",
  "timestamp": "2026-10-01T10:00:00"
}
```

---

# 16. Admin system-log APIs

## 16.1 Search system logs

```http
GET /api/v1/admin/system-logs
```

Filters:

```text
role
activity
fromDate
toDate
name
phone
email
```

System logs are audit records and are not exposed through destructive delete APIs.

### Request example

```json
{
  "role": "MANAGER",
  "activity": "PRODUCT_UPDATE",
  "fromDate": "2026-10-01",
  "toDate": "2026-10-01",
  "page": 0,
  "size": 20
}
```

### Success response — 200

```json
{
  "statusCode": 200,
  "data": {
    "content": [
      {
        "logId": 9001,
        "userId": 2001,
        "activity": "PRODUCT_UPDATE",
        "description": "Updated product 100",
        "createdAt": "2026-10-01T09:40:00"
      }
    ],
    "meta": {
      "page": 0,
      "size": 10,
      "totalElements": 1,
      "totalPages": 1
    }
  },
  "message": "Thành công",
  "timestamp": "2026-10-01T10:00:00"
}
```

### Failure response — 403

```json
{
  "statusCode": 403,
  "data": null,
  "message": "You are not authorized to view system logs.",
  "timestamp": "2026-10-01T10:00:00"
}
```

## 16.2 Get system-log detail

```http
GET /api/v1/admin/system-logs/{logId}
```

### Request example

```json
{}
```

### Success response — 200

```json
{
  "statusCode": 200,
  "data": {
    "logId": 9001,
    "userId": 2001,
    "activity": "PRODUCT_UPDATE",
    "entityType": "PRODUCT",
    "entityId": 100,
    "description": "Updated product 100",
    "oldValue": "...",
    "newValue": "...",
    "createdAt": "2026-10-01T09:40:00"
  },
  "message": "Thành công",
  "timestamp": "2026-10-01T10:00:00"
}
```

### Failure response — 404

```json
{
  "statusCode": 404,
  "data": null,
  "message": "System log not found.",
  "timestamp": "2026-10-01T10:00:00"
}
```

---

# 17. Notification APIs

## 17.1 Get own notifications

```http
GET /api/v1/notifications
```

Required business channels: website, Zalo, email. External provider details are `TBD`.

### Request example

```json
{
  "page": 0,
  "size": 20
}
```

### Success response — 200

```json
{
  "statusCode": 200,
  "data": {
    "content": [
      {
        "notificationId": 5001,
        "title": "Đơn hàng đã được tính tiền",
        "message": "Đơn hàng #10025 đã có tổng tiền.",
        "isRead": false,
        "createdAt": "2026-10-01T09:50:00"
      }
    ],
    "meta": {
      "page": 0,
      "size": 10,
      "totalElements": 1,
      "totalPages": 1
    }
  },
  "message": "Thành công",
  "timestamp": "2026-10-01T10:00:00"
}
```

### Failure response — 401

```json
{
  "statusCode": 401,
  "data": null,
  "message": "Authentication is required.",
  "timestamp": "2026-10-01T10:00:00"
}
```

## 17.2 Mark notification as read

```http
POST /api/v1/notifications/{notificationId}/read
```

### Request example

```json
{}
```

### Success response — 200

```json
{
  "statusCode": 200,
  "data": {
    "notificationId": 5001,
    "isRead": true
  },
  "message": "Thành công",
  "timestamp": "2026-10-01T10:00:00"
}
```

### Failure response — 404

```json
{
  "statusCode": 404,
  "data": null,
  "message": "Notification not found.",
  "timestamp": "2026-10-01T10:00:00"
}
```

## 17.3 Mark all notifications as read

```http
POST /api/v1/notifications/read-all
```

### Request example

```json
{}
```

### Success response — 200

```json
{
  "statusCode": 200,
  "data": {
    "updatedCount": 5
  },
  "message": "Thành công",
  "timestamp": "2026-10-01T10:00:00"
}
```

### Failure response — 401

```json
{
  "statusCode": 401,
  "data": null,
  "message": "Authentication is required.",
  "timestamp": "2026-10-01T10:00:00"
}
```

---

# 18. Authorization matrix

| Operation | Guest | Customer | Manager | Admin |
|---|---:|---:|---:|---:|
| Browse active products | Yes | Yes | Yes | Yes |
| View product price | No | Permission-controlled | Yes | Yes |
| Create order | No | Yes | No | No |
| View own orders | No | Yes | No | No |
| Cancel own order | No | Yes, within rule | No | No |
| Confirm own calculated order | No | Yes | No | No |
| Create/update/lock/unlock/delete categories | No | No | Yes | TBD |
| Create/update/lock/unlock/delete products | No | No | Yes | TBD |
| Calculate order | No | No | Yes | TBD |
| Update order status | No | No | Yes | TBD |
| Manage users | No | No | No | Yes |
| Create Manager Google account | No | No | No | Yes |
| Manage permissions | No | No | No | Yes |
| View system logs | No | No | No | Yes |
| Configure system settings | No | No | No | Yes |
| Manage brands (CRUD/Lock) | No | No | View only | Yes |
| Manage units (CRUD/Lock) | No | No | View only | Yes |
| Manage colors (CRUD/Lock) | No | No | View only | Yes |

`TBD` means the requirements do not yet define the full Admin scope for product/category/order operations.

---

# 19. Price visibility API rules

Price visibility is controlled by permission, not by hard-coded role checks in the controller.

```text
PRODUCT_PRICE_VIEW
```

Server-side rule:

```text
hasAuthority("PRODUCT_PRICE_VIEW")
```

### When the caller has the permission

The API may return:

```json
{
  "prices": {
    "m": 100000,
    "tam": 150000
  }
}
```

### When the caller does not have the permission

The API must not leak price values through any alternative field or endpoint.

```json
{
  "prices": null,
  "priceVisible": false
}
```

Order totals are a separate business concern. Once Manager calculates an order, the Customer may see that order's `totalAmount` so the Customer can decide whether to confirm.

---

# 20. Product price map contract

Product price is represented as a map/dictionary:

```text
Map<unit, price>
```

Example:

```json
{
  "m": 100000,
  "tam": 150000,
  "kg": 75000
}
```

Rules:

1. The map key is the configured selling unit.
2. The map value is the price for that unit.
3. Admin/Manager may manage price data according to the authorization rules.
4. Customer visibility depends on `PRODUCT_PRICE_VIEW`.
5. Order calculation must snapshot the selected unit and unit price into the order item so later product-price changes do not change historical order totals.

---

# 21. Lock and delete API contract

For resources that support both operations:

```text
POST   /resource/{id}/lock
POST   /resource/{id}/unlock
DELETE /resource/{id}
```

Behavior:

```text
LOCK
  -> reversible
  -> resource can later be UNLOCKED

DELETE
  -> permanent
  -> no RESTORE endpoint
  -> recreate the resource if it is needed again
```

The following business resources currently use this pattern where applicable:

```text
Category
Product
ProductVariant
Brand
Unit
Color
User account
```

Product images and customer addresses support permanent deletion where allowed.

System logs are append-only and are not physically deleted through the API.

---

# 22. Order status contract

Current status flow:

```text
PENDING
   ↓
WAITING_FOR_CUSTOMER_CONFIRMATION
   ↓
CONFIRMED
   ↓
PROCESSING
   ↓
SHIPPING
   ↓
COMPLETED
```

Cancellation branch:

```text
PENDING --------------------┐
WAITING_FOR_CUSTOMER_CONFIRMATION -> CANCELLED
CONFIRMED ------------------┘
```

Customer can cancel within the configured cancellation window. Manager can cancel according to the business rules. Cancellation reason is optional for every role.

Every status transition must create an `OrderStatusHistory` record.

---

# 23. Redis-related API behavior

Redis is used for temporary state and access token validation; PostgreSQL remains the authoritative store for all business data.

Relevant data:

```text
OTP
    -> Redis
    -> TTL

Access Token state
    -> Redis
    -> key = auth:access:{jti}
    -> TTL = JWT lifetime
```

Orders and order items are stored and processed directly in PostgreSQL without an external Redis order queue.

---

# 24. API implementation rules

1. Every controller response must use the standard envelope:

```json
{
  "statusCode": 200,
  "data": {},
  "message": "Thành công",
  "timestamp": "20xx-02-28T10:00:00"
}
```

2. Never add a `success: true/false` property.
3. Every endpoint documented in this file must have a concrete JSON success example and a concrete JSON failure example.
4. Use `201` for successful resource creation.
5. Use `200` for successful reads, state changes, and deletes so every API still returns the JSON envelope.
6. Validation and exception handling must be centralized where possible.
7. Authorization must be checked on the server.
8. Never trust a `userId` supplied by the client when the authenticated user identity is already available in the JWT/security context.
9. Never expose product prices to callers without `PRODUCT_PRICE_VIEW`.
10. Refresh-token values must not be returned in JSON; use the secure HttpOnly cookie contract.
11. Refresh-token storage must use a hash in PostgreSQL.
12. Access-token state must be registered in Redis by JWT `jti`.
13. Lock operations are reversible; delete operations are permanent.
14. Product price is stored/exposed as a unit-to-price map.
15. Any change to endpoint, request field, response field, authorization rule, or status transition must update this file together with the related architecture/database documentation.
