Write tests for the specified feature module. Read the source code first, then generate tests.

## Step 1: Read Before Writing
- Read the feature's source code: entity, service, serviceImpl, controller, DTOs
- Read `CONTEXT.md` of the module (if exists) to understand trade-offs and edge cases
- Read `docs/API_SPEC.md` for expected request/response formats
- Identify all code paths: happy path, error cases, edge cases, validation

## Step 2: Unit Tests — `{Feature}ServiceImplTest.java`

Location: `src/test/java/{same package as source}/`

```java
@ExtendWith(MockitoExtension.class)
class {Feature}ServiceImplTest {

    @Mock
    private {Feature}Repository {feature}Repository;
    // @Mock other dependencies...

    @InjectMocks
    private {Feature}ServiceImpl {feature}Service;
}
```

### Required Coverage Per Method

**Create method:**
- ✅ Success — valid input → entity saved → correct response returned
- ✅ Duplicate — conflicting data exists → throws InvalidRequestException
- ✅ Related entity not found — e.g. companyId invalid → throws ResourceNotFoundException
- ✅ Verify: repository.save() called exactly once
- ✅ Verify: password encoded (if applicable)

**GetById method:**
- ✅ Found — returns correct response DTO
- ✅ Not found — throws ResourceNotFoundException with meaningful message
- ✅ Verify: response DTO fields match entity fields

**GetAll / List method:**
- ✅ Returns paginated result with correct meta
- ✅ Empty result — returns empty list, not null

**Update method:**
- ✅ Success — entity found, fields updated, saved
- ✅ Not found — throws ResourceNotFoundException
- ✅ Partial update — only provided fields change, others remain
- ✅ Conflict — e.g. email changed to existing one → throws exception

**Delete method:**
- ✅ Success — entity found and deleted
- ✅ Not found — throws ResourceNotFoundException
- ✅ Side effects — e.g. refresh tokens revoked (if user delete)

### Unit Test Rules
- Mock ONLY direct dependencies (repository, other services)
- Test ONE behavior per test method
- Naming: `methodName_scenario_expectedResult`
- `@DisplayName("Should ...")` on every test — describe behavior, not code
- Use `assertThrows()` for exception cases, verify exception message
- Use `verify()` to confirm interactions (save called, delete called)
- NEVER call real database — everything is mocked

## Step 3: Integration Tests — `{Feature}ControllerTest.java`

Location: `src/test/java/{same package as source}/`

```java
@SpringBootTest
@AutoConfigureMockMvc
class {Feature}ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // Helper: get valid JWT for authenticated requests
    private String getAuthToken() { ... }
}
```

### Required Coverage Per Endpoint

**POST (create):**
- ✅ 201 — valid request body → created successfully
- ✅ 400 — missing required fields → validation error with field details
- ✅ 400 — invalid format (e.g. bad email) → validation error
- ✅ 409 — duplicate data → conflict error
- ✅ 401 — no token → unauthorized

**GET /{id} (single):**
- ✅ 200 — exists → correct response body
- ✅ 404 — not exists → not found error
- ✅ 401 — no token → unauthorized

**GET (list):**
- ✅ 200 — returns paginated result with meta (page, pageSize, pages, total)
- ✅ 200 — with query params (page=2&size=5) → correct pagination
- ✅ 200 — empty result → empty array, not error

**PUT (update):**
- ✅ 200 — valid update → updated successfully
- ✅ 400 — invalid body → validation error
- ✅ 404 — entity not found
- ✅ 401 — no token → unauthorized

**DELETE /{id}:**
- ✅ 200 — exists → deleted, data is null
- ✅ 404 — not exists → not found error
- ✅ 401 — no token → unauthorized

### Integration Test Rules
- Test full HTTP cycle: request → security → controller → service → repository → response
- Use `mockMvc.perform(...)` with proper content type and auth headers
- Verify: `.andExpect(status().isOk())`, `.andExpect(jsonPath("$.statusCode").value(200))`
- Verify response structure matches ApiResponse format (statusCode, data, message, timestamp)
- Use `@Sql` or `@BeforeEach` to set up test data
- Clean up test data after each test (`@Transactional` on test class auto-rollbacks)
- NEVER depend on data from another test — each test is independent

## Step 4: Utility / Helper Tests (if applicable)

For classes in `util/` or `security/`:
```java
class SecurityUtilTest {
    // Test pure logic — no Spring context needed
    // No @ExtendWith, no @SpringBootTest — plain JUnit
}
```

Only test utils that have actual logic (parsing, formatting, validation helpers).
Skip utils that are simple wrappers.

## Step 5: Verify
- All tests pass: `mvn test`
- No test depends on execution order
- No test calls external services or real database (unit tests)
- Test names clearly describe WHAT is being tested and EXPECTED result
- Coverage: every public service method has at least happy path + error case