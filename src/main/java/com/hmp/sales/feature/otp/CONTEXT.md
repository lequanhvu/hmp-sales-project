# Phone OTP Verification — Implementation Context
> Written: 2026-10-25 | Author: @antigravity

## Business Context
Provides phone number verification for Vietnamese individual customers before placing orders or updating user profiles. It prevents spam order placement and guarantees accurate contact information for order fulfillment.

## Technical Decisions
- **Redis for OTP state & rate-limiting**: All verification tokens, resend cooldowns, and wrong-attempt counters use Redis with short TTLs (`otp:phone:{phone}`, `otp:rate:{phone}`, `otp:attempts:{phone}`) to keep ephemeral auth state out of the relational database.
- **Dual persistence on success**: Upon successful OTP verification, the phone number is immediately persisted into the `User` entity in PostgreSQL (if authenticated), while a 10-minute temporary verification proof (`otp:verified:{phone}`) is retained in Redis for order placement validation.
- **Single-use OTP consumption**: OTP key and failed attempt counters are deleted immediately upon successful verification to prevent replay attacks.
- **Rate limiting and attempt caps**: Enforces a 60-second cooldown between resend requests and locks out verification after 5 failed attempts (deleting the OTP to enforce resending) to protect against brute-force attacks.
- **SecureRandom 6-digit generation**: Generates cryptographically secure 6-digit numeric codes with leading-zero formatting (`%06d`).

## Considered and Rejected
- **Storing OTP in PostgreSQL table**: Rejected because OTPs are short-lived (5 min TTL) high-frequency ephemeral data; Redis TTL is cleaner and prevents database bloat.
- **JWT-based phone verification token**: Rejected because server-side Redis key (`otp:verified:{phone}`) with TTL can be easily validated and consumed by `OrderService` atomically without extra cryptographic parsing overhead.
- **Third-party SMS Gateway in early V1**: Rejected for initial phase; OTP is logged in DEBUG level (with code hidden in general logs) to facilitate rapid development and testing without paid SMS vendor costs.

## Dependencies
- Depends on: `feature/user` (`UserRepository`), `config` (`AppProperties`), `common/exception` (`BusinessRuleException`, `OtpVerificationException`), `StringRedisTemplate`.
- Depended by: `feature/order` (validates `isVerified` and calls `consumeVerification` upon order placement), `UserController` (profile phone updates).

## Known Limitations
- ⚠️ SMS provider integration (e.g. eSMS, SpeedSMS, Zalo ZNS) is stubbed in V1; OTP is generated in Redis and debug-logged rather than physically transmitted via telco network.

## Refactor Log
(Add entries here when significant changes are made. Do NOT edit sections above.)
