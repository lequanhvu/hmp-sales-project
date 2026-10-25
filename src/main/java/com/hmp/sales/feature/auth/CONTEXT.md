# Authentication & Token Management — Implementation Context
> Written: 2026-10-25 | Author: @antigravity

## Business Context
Provides identity verification and session management for Vietnamese retail sales operations. Customers authenticate passwordless via Google OAuth with automatic account provisioning, while session continuity and revocability are enforced via stateless JWT access tokens and secure, rotatable refresh tokens.

## Technical Decisions
- **Nimbus JOSE + HS256**: Lightweight, standard-compliant JWT creation and validation without heavyweight dependencies or external identity provider overhead.
- **Access token JTI registration in Redis**: Registered in Redis (`auth:access:{jti}`) with matching expiration TTL to enable instantaneous, server-side revocation on logout or account lock while keeping JWTs stateless.
- **Minimal JWT claims (no permissions in token)**: Access token only carries `sub`, `email`, and `roles` (e.g. `ROLE_CUSTOMER`) to keep token payload small and eliminate stale authorization; permissions are resolved dynamically via in-memory RAM cache.
- **Opaque SHA-256 hashed Refresh Tokens with rotation**: Refresh tokens are stored only as SHA-256 hashes in PostgreSQL with family ID tracking; rotating on each refresh revokes the prior token atomically and revokes the whole family on replay detection.
- **HttpOnly, Secure, SameSite Cookie for Refresh Tokens**: Shields refresh tokens against XSS attacks in client browsers while keeping access tokens in client memory.
- **Spring RestClient for Google TokenInfo**: Directly queries `https://oauth2.googleapis.com/tokeninfo` instead of adding the heavy Google API Client SDK.

## Considered and Rejected
- **Permissions inside JWT payload**: Rejected because token size bloats and changing permissions requires re-authenticating the user.
- **Storing Refresh Tokens in Redis**: Rejected because refresh tokens are durable session records that must survive Redis cache flushes and need PostgreSQL ACID transactions for token family revocation.
- **Plaintext Refresh Tokens in DB**: Rejected to avoid credential leakage in case of database backup or read compromise.
- **Local username/password login for Customers**: Rejected because confirmed business requirements mandate Google OAuth for customers to minimize onboarding friction.

## Dependencies
- Depends on: `feature/user` (`User`, `Role`, `UserRepository`, `RoleRepository`), `config` (`AppProperties`), `common/exception`.
- Depended by: All protected API endpoints requiring OAuth2 Resource Server Bearer authentication, frontend client.

## Known Limitations
- ⚠️ Real Google ID Token verification requires external network connectivity to Google TokenInfo; mock client is used for isolated unit and integration testing.
- ⚠️ Local password authentication is not yet exposed via public endpoints in V1; only Google OAuth authentication is active for customers.

## Refactor Log
(Add entries here when significant changes are made. Do NOT edit sections above.)
