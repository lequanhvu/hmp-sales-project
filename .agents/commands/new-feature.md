Create a new feature module. Follow these steps in exact order.

## Step 1: Understand Context
- Read `AGENTS.md` → project overview, source of truth, and out-of-scope boundaries
- Read `.agents/rules/project-rules.md` → coding conventions, security, and persistence rules
- Read `architecture.md` → understand where this feature fits in the overall architecture
- Read `database.md` → check if entity/table schema already exists, primary/foreign keys, and constraints
- Read `api-spec.md` → check if endpoints, request/response contracts are already defined
- Read `project-status.md` → check current phase progress and milestone status
- Clarify any ambiguous requirements before writing code (mark undecided details as `TBD`)

## Step 2: Create Package Structure
```
feature/{feature_name}/
├── {Feature}.java                    # Entity
├── {Feature}Controller.java          # REST controller
├── {Feature}Service.java             # Service interface
├── {Feature}ServiceImpl.java         # Service implementation (implements {Feature}Service)
├── {Feature}Repository.java          # Spring Data JPA repository
└── dto/
    ├── Create{Feature}Request.java   # Create request DTO (record)
    ├── Update{Feature}Request.java   # Update request DTO (record)
    └── {Feature}Response.java        # Response DTO (record + fromEntity mapping)
```

Test files (mirror same package under `src/test/java`):
```
feature/{feature_name}/
├── {Feature}ServiceImplTest.java     # Unit test — service business logic
└── {Feature}ControllerTest.java      # Integration test — full HTTP flow & authorization
```

## Step 3: Implement in This Order
1. **Entity**
   - Fields, explicit JPA annotations (`@Table`, `@Column` with nullable/length/unique).
   - `@Enumerated(EnumType.STRING)` for all enum types (never ORDINAL).
   - `FetchType.LAZY` on all relationships (`@OneToMany`, `@ManyToOne`, `@ManyToMany`).
   - Monetary values (VND) stored as `BIGINT` or `INT` (NEVER floating-point `Double`/`Float`).
   - Audit fields (`createdAt`, `updatedAt`) and soft-delete flag (`isDeleted`) if applicable.
   - Use `@Getter`, `@Setter`, explicit `equals`/`hashCode` — NEVER `@Data` on entities.
2. **Repository**
   - Extend `JpaRepository<{Feature}, ID>`.
   - Add custom query methods returning `Optional<T>` for single results.
   - Use `join fetch` or `@EntityGraph` where needed to avoid N+1 query issues.
3. **DTOs (Java Records)**
   - Request records with Jakarta Validation annotations (`@NotBlank`, `@NotNull`, `@Size`, `@Min`, etc.).
   - Response record with `fromEntity()` mapper (or dedicated mapper).
   - Never expose sensitive fields (passwords, OTPs, provider tokens, internal secrets).
   - **Price Visibility**: If feature exposes product price, omit/nullify price in response when caller lacks `PRODUCT_PRICE_VIEW`.
4. **Service Interface**
   - Define business contract (method signatures only).
   - Name: `{Feature}Service.java`.
5. **Service Implementation**
   - Name: `{Feature}ServiceImpl.java` implementing `{Feature}Service`.
   - Pure business logic, authorization checks, validation, and entity ↔ DTO conversion.
   - `@Transactional` on all write methods; read-only transactions on read methods.
   - **No Physical Delete**: Master data referenced by historical orders must not be hard deleted (use soft delete or toggle status).
   - **Order Immutability**: Historical order snapshots must remain immutable.
6. **Controller**
   - Name: `{Feature}Controller.java`.
   - Thin controller: receives HTTP request → calls `{Feature}Service` interface → wraps in `ApiResponse<T>` → returns.
   - Inject `{Feature}Service` interface, NEVER `{Feature}ServiceImpl`.
   - `@Valid` on every `@RequestBody`.
   - No try/catch in controller — delegate error handling to `GlobalExceptionHandler`.

## Step 4: Write Tests

Using `/commands/write-tests.md` instruction.

### Unit Tests — `{Feature}ServiceImplTest.java`
```java
@ExtendWith(MockitoExtension.class)
```
Mock all dependencies (repository, other service interfaces). Test service business logic only.

Required test cases per service method:
- **Create**: success, validation fail (duplicate/conflict → `BusinessRuleException`), related entity not found (`ResourceNotFoundException`).
- **GetById**: success (found), not found (`ResourceNotFoundException`).
- **GetAll**: returns populated list/page, returns empty list/page.
- **Update**: success, not found, business rule validation fail.
- **Delete / Deactivate**: success (soft delete/status toggle), not found, fail when entity has active/historical order dependencies.
- **Domain calculations**: verify manufacturing formula `UnitPrice × Size × Quantity` if applicable.
- **Price visibility**: verify price omitted when caller lacks `PRODUCT_PRICE_VIEW`.

Naming: `methodName_scenario_expectedResult`
Every test has `@DisplayName` describing expected behavior.

### Integration Tests — `{Feature}ControllerTest.java`
```java
@SpringBootTest
@AutoConfigureMockMvc
```
Test full HTTP flow: request → controller → service → repository → response.

Required test cases per endpoint:
- **POST**: 201 Created on success, 400 on validation error, 409/422 on business rule conflict, 401 on unauthenticated, 403 on missing permission.
- **GET /{id}**: 200 OK on success, 404 Not Found, 401 unauthenticated, 403 forbidden.
- **GET (list/search)**: 200 OK with pagination & sorting parameters.
- **PUT / PATCH**: 200 OK on success, 400 validation error, 404 not found, 403 forbidden.
- **DELETE**: 200 OK or 204 No Content on soft delete, 404 not found, 400/409 when constrained by historical orders.

Verify: HTTP status code, `ApiResponse` envelope (`statusCode`, `data`, `message`), and content type.

### What NOT to Test
- Simple getters/setters on entities or records.
- Spring Data JPA built-in methods (`findById`, `save` — already tested by framework).
- Trivial DTOs with no custom logic.
- Do NOT make calls to live external providers (SMS, Zalo, real Google OAuth servers); use mocks/stubs.

## Step 5: Follow These Rules
- **Interface + Impl**: Service must be an interface, implementation class named `{Feature}ServiceImpl`.
- **Constructor injection only**: `private final` fields with explicit constructor (no `@Autowired` on fields).
- **Controller injects Service Interface**: NEVER inject concrete `ServiceImpl`.
- **Response wrapper**: All controller responses wrapped in `ApiResponse<T>`.
- **Validation**: `@Valid` on every `@RequestBody`.
- **JPA Fetching**: `FetchType.LAZY` on all relationships.
- **Exceptions**: Use domain exceptions (`ResourceNotFoundException`, `BusinessRuleException`, `UnauthorizedOperationException`, `InvalidOrderStateException`, `OtpVerificationException`).
- **DTO separation**: Entities are NEVER returned directly from REST controllers.
- **Money type**: Vietnamese currency (VND) stored as `BIGINT` or `INT` (never `Double` or `Float`).
- **Redis boundary**: Redis for ephemeral data (OTP, active access token `jti`); never store durable business entities or refresh tokens in Redis. Orders are managed directly in PostgreSQL.
- **Scope protection**: Do NOT implement out-of-scope V1 features (online payment, inventory, coupons, reviews, quotation, laser cutting, warranty, blogs).

## Step 6: Update Documentation
- Update `api-spec.md` with new endpoints and request/response DTO schemas.
- Update `database.md` if schema, tables, or columns were modified.
- Update `architecture.md` if business flows or integration steps changed.
- Create `CONTEXT.md` inside the feature package (`feature/{feature_name}/CONTEXT.md`) using `/write-context`.
- Update `project-status.md` using `/update-status`.

## Step 7: Verify
- Code compiles with no errors (`mvn clean compile`).
- All tests pass (both existing and new tests: `mvn test`).
- Adhere to size guidelines:
  - Methods: `< 50 lines` (extract complex logic into private helper methods)
  - Controllers, DTOs, Repositories: `< 200 lines`
  - Complex Services, Entities, Config: target `< 300 lines` (soft limit `< 400 lines`)
  - Test files (`*Test.java`): target `< 500 lines` (prioritize scenario coverage over arbitrary splitting)
- Check commit & coding guidelines in `.agents/rules/project-rules.md` (Section 17).