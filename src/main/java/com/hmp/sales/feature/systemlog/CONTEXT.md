# System Audit Log — Implementation Context
> Written: 2026-10-25 | Author: @antigravity

## Business Context
Provides business and security audit trail recording for sensitive administrative actions (role permission updates, user locking/unlocking, manager provisioning, and system settings modifications) to satisfy accountability and traceability requirements.

## Technical Decisions
- **Interface + Stub Implementation in early phase**: Defined `SystemLogService` contract with `log` and `logWithChange` methods early on so that calling services (`RoleServiceImpl`, `AdminUserServiceImpl`) can inject and invoke audit logging without being blocked by Phase 5 database table implementation.
- **SLF4J logger fallback**: The stub implementation logs structured audit records (`[SystemLog] actor=... | action=... | entity=...`) to technical application logs until the PostgreSQL `system_logs` table and repository are implemented in Phase 5.
- **Pre-change and post-change support**: `logWithChange` supports capturing `oldValue` and `newValue` snapshots (e.g. JSON strings or concise summaries) for critical state alterations.

## Considered and Rejected
- **AOP / Method interceptor-based logging**: Rejected because explicit programmatic logging at business boundaries provides clearer context, accurate entity IDs, and localized Vietnamese descriptions without magic annotations.
- **Blocking caller on database write failure**: Rejected (planned for async/non-blocking or decoupled persistence in Phase 5) so that audit logging issues do not abort critical administrative transactions.

## Dependencies
- Depends on: SLF4J (Spring Boot core). In Phase 5, will depend on PostgreSQL `SystemLog` entity and repository.
- Depended by: `feature/user` (`RoleServiceImpl`, `AdminUserServiceImpl`), and future administrative services (`SettingService`).

## Known Limitations
- ⚠️ In Phase 1–4, audit records are output to application console logs via SLF4J; durable persistence to PostgreSQL `system_logs` table and query API will be completed in Phase 5.

## Refactor Log
(Add entries here when significant changes are made. Do NOT edit sections above.)
