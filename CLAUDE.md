# CLAUDE.md

Hướng dẫn cho Claude Code khi làm việc với repository này. Đây là tài liệu kiến trúc tổng thể — mọi phát triển tính năng PHẢI tuân theo.

## Project Overview

Spring Boot 4.0.5 REST API project (`com.priz.base`). Java 21, Maven, MySQL (`priz_base`), Lombok, Spring Data JPA, Spring Security (stateless JWT), Spring Actuator, SpringDoc OpenAPI.

Phụ thuộc vào module `com.priz:common` (shared library) cho JWT, exceptions, SecurityContext, và `@Secured` annotation.

## Build & Run

```bash
mvnw.cmd spring-boot:run                    # Chạy app
mvnw.cmd compile                             # Compile
mvnw.cmd test                                # Chạy tất cả test
mvnw.cmd test -Dtest=ClassName               # Chạy 1 test class
mvnw.cmd test -Dtest=ClassName#methodName    # Chạy 1 test method
mvnw.cmd package                             # Build JAR
```

> **Lưu ý:** `common` module phải được install trước khi build `base`:
> ```bash
> cd ../common && mvnw.cmd install
> ```

---

## Architecture: Hexagonal (Ports & Adapters)

Base package: `com.priz.base`

### Layer Map — Nơi tìm kiếm & nơi đặt code

| Layer | Package | Mục đích | Chứa gì |
|-------|---------|----------|---------|
| **Common** | `common/` | Shared kernel (base-local) | JPA `@MappedSuperclass` base (`model/BaseModel`), `GlobalExceptionHandler` (`exception/`), API response wrappers (`response/`), pagination DTOs (`request/`), utility classes (`util/`), AOP logging (`logger/`), storage services (`storage/`) |
| **Config** | `config/` | Cấu hình framework | Spring Security filter chain (`security/SecurityConfig`), JPA auditing (`database/`), OpenAPI config |
| **Infrastructure** | `infrastructure/` | Outbound adapters (kỹ thuật) | `SecuredAspect` AOP (`security/aspect/`), messaging producers, external API clients |
| **Domain** | `domain/` | Physical data layer | JPA `@Entity` models (`model/`), Spring Data repositories (`repository/`), JPA `AttributeConverter`s (`converter/`). Tổ chức: `domain/{engine}/{db_name}/` |
| **Application** | `application/` | Business logic core | Feature modules (`features/`) với Service interface + impl, DTOs, business entities, converters, helpers, constants. Admin logic (`admin/`), global constants (`constant/`) |
| **Interfaces** | `interfaces/` | Inbound adapters | REST controllers (`rest/`), gRPC services (`grpc/`), WebSocket handlers (`socket/`), SQS consumers (`sqs/`), scheduled workers (`worker/`), webhook endpoints (`webhook/`) |
| **ELK Stack** | `elk/`, `domain/elasticsearch/` | Logging & Search | Logstash pipeline configs, Elasticsearch documents and repositories |

### Directory Tree

```
src/main/java/com/priz/base/
├── BaseApplication.java                     # scanBasePackages: com.priz.base + com.priz.common
│
├── common/                              # BASE-LOCAL SHARED KERNEL
│   ├── model/                           # BaseModel (@MappedSuperclass, dùng chung mọi DB JPA)
│   ├── exception/                       # GlobalExceptionHandler (exception classes ở com.priz.common)
│   ├── logger/                          # RequestLoggingAspect (AOP)
│   ├── request/                         # PageRequestDto
│   ├── response/                        # ApiResponse<T>, PageResponse<T>
│   ├── storage/                         # LocalStorageService
│   └── util/                            # DateTimeUtil, JsonUtil
│
├── config/                              # FRAMEWORK CONFIG
│   ├── database/                        # JpaAuditingConfig
│   ├── security/                        # SecurityConfig (filter chain, BCryptPasswordEncoder)
│   └── OpenApiConfig.java
│
├── infrastructure/                      # OUTBOUND ADAPTERS
│   └── security/
│       └── aspect/                      # SecuredAspect (AOP — WebMVC-specific)
│
├── domain/                              # PHYSICAL DATA LAYER
│   └── mysql/
│       └── priz_base/
│           ├── model/                   # UserModel, RefreshTokenModel, FileModel
│           └── repository/              # UserRepository, RefreshTokenRepository, FileRepository
│
├── application/                         # BUSINESS LOGIC
│   ├── features/
│   │   ├── auth/                        # AuthService, impl/, dto/
│   │   ├── users/                       # UserService, impl/, dto/, converter/
│   │   └── files/                       # FileService, impl/, dto/, converter/, helper/
│   └── admin/
│       └── internal/                    # Generic Admin CRUD system
│           ├── AdminService.java        # Interface
│           ├── impl/                    # AdminServiceImpl
│           ├── registry/                # AdminEntityRegistry, AdminEntityRegistration
│           │   ├── annotation/          # @AdminManaged, @AdminHidden
│           │   └── impl/               # AdminEntityRegistryImpl (auto-discover)
│           ├── introspector/            # EntitySchemaIntrospector (schema → FE)
│           ├── specification/           # GenericSpecificationBuilder (dynamic filter)
│           ├── converter/              # AdminEntityConverter (entity ↔ Map)
│           └── dto/                    # FilterOperator, FilterCondition, AdminFilterRequest,
│                                       # FieldSchema, TableSchemaResponse
│
└── interfaces/                          # INBOUND ADAPTERS
    └── rest/                            # HealthController, AuthController, UserController,
                                         # FileController, AdminController
```

### Shared code từ `com.priz.common` (không nằm trong base)

Các class sau **đã được chuyển sang module `common`** và import từ đó:

| Class | Package trong common | Ghi chú |
|-------|----------------------|---------|
| `BusinessException` | `com.priz.common.exception` | Base exception |
| `UnauthorizedException` | `com.priz.common.exception` | HTTP 401 |
| `ForbiddenException` | `com.priz.common.exception` | HTTP 403 |
| `ResourceNotFoundException` | `com.priz.common.exception` | HTTP 404 |
| `SecurityContext` | `com.priz.common.security` | Data class chứa user info |
| `SecurityContextHolder` | `com.priz.common.security` | ThreadLocal holder |
| `@Secured` | `com.priz.common.security.annotation` | Custom security annotation |
| `JwtService` | `com.priz.common.security.jwt` | JWT generation & validation |

---

## Strict Rules — PHẢI tuân theo

1. **Controllers CHỈLÝ nằm trong `interfaces/rest/`**. Không bao giờ trong `controller/` hay `application/`.
2. **JPA `@Entity` CHỈLÝ nằm trong `domain/{engine}/{db_name}/model/`**. Không bao giờ trong `application/` hay `common/`. **`BaseModel`** (`@MappedSuperclass`) nằm trong `common/model/` — dùng chung cho mọi persistence unit / DB.
3. **Repositories CHỈLÝ nằm trong `domain/{engine}/{db_name}/repository/`**. Không bao giờ cạnh services.
4. **Business logic nằm trong `application/features/{feature}/impl/`**. Controller phải thin — chỉ validate input và delegate.
5. **DTOs nằm trong `application/features/{feature}/dto/`**. Request và Response objects riêng biệt.
6. **Converters nằm trong `application/features/{feature}/converter/`**. Map giữa Model ↔ DTO.
7. **Tất cả API response dùng `ApiResponse<T>` wrapper** từ `common/response/`.
8. **Tất cả list API dùng `PageResponse<T>`** với `PageRequestDto` cho pagination.
9. **Protected endpoints dùng `@Secured`** từ `com.priz.common.security.annotation` (KHÔNG PHẢI Spring @Secured).
10. **Services dùng interface-based pattern**: khai báo interface trong feature root, implement trong `impl/`.
11. **Security context**: dùng `SecurityContextHolder.getCurrentUserId()` từ `com.priz.common.security` để lấy user đang đăng nhập.
12. **TUYỆT ĐỐI KHÔNG dùng inline import trong khai báo field/variable**. Sai: `private java.time.Instant foo;`. Đúng: khai báo `import java.time.Instant;` ở đầu file, rồi dùng `private Instant foo;`.
13. **Exceptions** phải import từ `com.priz.common.exception.*` — KHÔNG tạo exception class mới trong `base/common/exception/`.

---

## Hướng dẫn thêm Feature Module mới

1. Tạo package: `application/features/{feature_name}/`
2. Tạo service interface: `{Feature}Service.java`
3. Tạo `impl/{Feature}ServiceImpl.java` với `@Service`
4. Tạo `dto/` với request/response classes
5. Tạo `converter/{Feature}Converter.java` (static utility class)
6. Tạo `helper/` nếu cần (Specification, helper functions)
7. Tạo controller trong `interfaces/rest/{Feature}Controller.java`
8. Nếu cần bảng DB mới: tạo model trong `domain/mysql/priz_base/model/` và repo trong `domain/mysql/priz_base/repository/`

**Template feature module:**
```
application/features/{feature_name}/
├── {Feature}Service.java           # Interface
├── impl/
│   └── {Feature}ServiceImpl.java   # @Service implementation
├── dto/
│   ├── {Action}Request.java        # Input DTOs
│   └── {Feature}Response.java      # Output DTOs
├── converter/
│   └── {Feature}Converter.java     # Model ↔ DTO mapping
├── helper/                          # (optional)
│   └── {Feature}Specification.java # JPA dynamic filter
└── constant/                        # (optional)
    └── {Feature}Constant.java      # Enum/Constant nội bộ
```

## Hướng dẫn thêm JPA Entity mới

1. Tạo trong `domain/mysql/priz_base/model/{Name}Model.java`
2. Extend `com.priz.base.common.model.BaseModel` → tự có `id` (UUID), audit fields
3. Tạo repo trong `domain/mysql/priz_base/repository/{Name}Repository.java`
4. Extend `JpaRepository<{Name}Model, String>` + `JpaSpecificationExecutor<{Name}Model>` nếu cần filter

---

## Admin Generic CRUD System

Hệ thống admin tự động CRUD cho mọi JPA entity — **không cần viết thêm service hay controller**.

### Expose entity vào admin API (2 bước)

**Bước 1:** Gắn `@AdminManaged` lên entity. Gắn `@AdminHidden` lên các field nhạy cảm:

```java
@AdminManaged                             // đăng ký vào admin system
@Entity
@Table(name = "products")
public class ProductModel extends BaseModel {

    @AdminHidden                          // ẩn khỏi response, filter, schema, update
    private String secretKey;

    // ... các field bình thường
}
```

**Bước 2:** Đảm bảo repository extends `JpaSpecificationExecutor`:

```java
public interface ProductRepository extends JpaRepository<ProductModel, String>,
        JpaSpecificationExecutor<ProductModel> { ... }
```

Xong. Entity tự xuất hiện tại `/api/admin/tables/products`.

### @AdminManaged
- `@Target(TYPE)`, gắn lên entity class
- Attribute `tableName` (optional): override tên trong API path. Nếu bỏ trống, lấy từ `@Table(name=...)`

### @AdminHidden
- `@Target(FIELD)`, gắn lên field
- Field bị ẩn hoàn toàn: không xuất hiện trong response, không dùng được làm filter, không hiển thị trong schema, bị bỏ qua khi update

### Field mặc định read-only (không cho phép cập nhật qua admin)
`id`, `createdAt`, `updatedAt`, `createdBy`, `updatedBy` — schema trả `updatable: false`

### Filter operators (21 operators)
| Nhóm | Operators |
|------|-----------|
| Equality | `EQUAL`, `NOT_EQUAL` |
| String | `CONTAINS`, `NOT_CONTAINS`, `STARTS_WITH`, `ENDS_WITH`, `LIKE`, `EQUALS_IGNORE_CASE` |
| Comparison | `GREATER_THAN`, `LESS_THAN`, `GREATER_THAN_OR_EQUAL`, `LESS_THAN_OR_EQUAL`, `BETWEEN` |
| Set | `IN`, `NOT_IN` |
| Null | `IS_NULL`, `IS_NOT_NULL` |
| Boolean | `IS_TRUE`, `IS_FALSE` |
| Empty | `IS_EMPTY`, `IS_NOT_EMPTY` |

Schema API trả về `supportedOperators` phù hợp cho từng kiểu dữ liệu:
- **String**: EQUAL, NOT_EQUAL, CONTAINS, NOT_CONTAINS, STARTS_WITH, ENDS_WITH, LIKE, EQUALS_IGNORE_CASE, IN, NOT_IN, IS_NULL, IS_NOT_NULL, IS_EMPTY, IS_NOT_EMPTY
- **Number (Long/Integer)**: EQUAL, NOT_EQUAL, GT, LT, GTE, LTE, BETWEEN, IN, NOT_IN, IS_NULL, IS_NOT_NULL
- **Instant**: EQUAL, NOT_EQUAL, GT, LT, GTE, LTE, BETWEEN, IS_NULL, IS_NOT_NULL
- **Boolean**: EQUAL, NOT_EQUAL, IS_TRUE, IS_FALSE, IS_NULL, IS_NOT_NULL
- **Enum**: EQUAL, NOT_EQUAL, IN, NOT_IN, IS_NULL, IS_NOT_NULL

### Ví dụ filter request
```json
POST /api/admin/tables/users/query
{
  "filters": [
    { "field": "role", "operator": "EQUAL", "value": "ADMIN" },
    { "field": "isActive", "operator": "IS_TRUE" },
    { "field": "createdAt", "operator": "GREATER_THAN", "value": "2025-01-01T00:00:00Z" },
    { "field": "email", "operator": "CONTAINS", "value": "@gmail" }
  ],
  "pagination": { "page": 0, "size": 20, "sortBy": "createdAt", "sortDirection": "DESC" }
}
```

---

## Security Model

- **JWT access tokens** (1 giờ) + **refresh tokens** (7 ngày, lưu trong DB)
- `JwtService` từ `com.priz.common.security.jwt` — generate & validate JWT
- Custom `@Secured` annotation (`com.priz.common.security.annotation`) xử lý bởi `SecuredAspect` (AOP, WebMVC)
- `@Secured` không param = bất kỳ user authenticated
- `@Secured(roles = "ADMIN")` = yêu cầu role cụ thể
- Spring Security filter chain set `permitAll()` — authorization hoàn toàn qua AOP
- User context: `SecurityContextHolder.get()` / `.getCurrentUserId()` từ `com.priz.common.security`
- Password hash: BCrypt

## Naming Conventions

| Loại | Pattern | Ví dụ |
|------|---------|-------|
| JPA Entity | `{Name}Model.java` | `UserModel`, `FileModel` |
| Repository | `{Name}Repository.java` | `UserRepository` |
| Service Interface | `{Name}Service.java` | `AuthService`, `FileService` |
| Service Impl | `{Name}ServiceImpl.java` | `AuthServiceImpl` |
| Controller | `{Name}Controller.java` | `AuthController` |
| Request DTO | `{Action}Request.java` | `LoginRequest`, `UpdateProfileRequest` |
| Response DTO | `{Name}Response.java` | `AuthResponse`, `FileDetailResponse` |
| Converter | `{Name}Converter.java` | `UserConverter`, `FileConverter` |
| Specification | `{Name}Specification.java` | `FileSpecification` |
| Base model | `common/model/BaseModel.java` | `@MappedSuperclass` chung mọi JPA entity |

## Configuration

| Key | File | Mô tả |
|-----|------|-------|
| `spring.datasource.*` | `application.yaml` | MySQL connection |
| `jwt.secret` | `application.yaml` | JWT signing secret (≥32 chars) |
| `jwt.access-token-expiration` | `application.yaml` | Access token TTL (ms) |
| `jwt.refresh-token-expiration` | `application.yaml` | Refresh token TTL (ms) |
| `file.upload-dir` | `application.yaml` | Thư mục lưu file upload |
| `spring.servlet.multipart.max-file-size` | `application.yaml` | Giới hạn file upload |

## API Endpoints

### Public (không cần token)
- `GET /api/health` — health check
- `POST /api/auth/register` — đăng ký
- `POST /api/auth/login` — đăng nhập
- `POST /api/auth/logout` — đăng xuất
- `POST /api/auth/refresh-token` — refresh access token
- `POST /api/auth/forgot-password` — yêu cầu reset password
- `POST /api/auth/reset-password` — reset password
- `GET /swagger-ui.html` — Swagger UI
- `GET /v3/api-docs` — OpenAPI spec
- `GET /actuator/health` — Actuator health

### Protected (cần `@Secured` + JWT token)
- `GET /api/users/me` — xem profile
- `PUT /api/users/me` — cập nhật profile
- `PUT /api/users/me/password` — đổi mật khẩu
- `DELETE /api/users/me` — xóa tài khoản
- `POST /api/files` — upload file (multipart)
- `GET /api/files/{id}/download` — download file
- `GET /api/files/{id}` — xem chi tiết file
- `POST /api/files/filter` — danh sách file (pagination + filter)
- `DELETE /api/files/{id}` — xóa file
- `POST /api/files/sync` — sync file metadata

### Admin only (cần `@Secured(roles = "ADMIN")`)
- `GET /api/admin/tables` — danh sách bảng đã đăng ký
- `GET /api/admin/tables/{tableName}/schema` — schema metadata (fields, types, operators) cho FE
- `POST /api/admin/tables/{tableName}/query` — filter + pagination với dynamic conditions
- `GET /api/admin/tables/{tableName}/{id}` — lấy một record
- `POST /api/admin/tables/{tableName}` — tạo record
- `PUT /api/admin/tables/{tableName}/{id}` — cập nhật partial record
- `DELETE /api/admin/tables/{tableName}/{id}` — xóa record

## Tech Stack

- **Spring Boot 4.0.5**, Java 21, Maven
- **`com.priz:common`** — shared JWT, exceptions, SecurityContext (local Maven install)
- **MySQL** `priz_base` (runtime), **H2** (test profile)
- **Lombok** project-wide
- **JJWT** 0.12.6 — JWT token (via `common`)
- **SpringDoc OpenAPI** 2.8.6 — Swagger UI
- **Spring Data JPA** + `JpaSpecificationExecutor` — dynamic filtering
- **BCrypt** — password hashing
- **JUnit 5** (Jupiter) + **Mockito** — unit testing
- **Spring Boot Test** (`@WebMvcTest`, `MockMvc`) — controller slice testing

---

## Testing

### Chạy tests

```bash
mvnw.cmd test                                # Tất cả tests
mvnw.cmd test -Dtest=ClassName               # 1 test class
mvnw.cmd test -Dtest=ClassName#methodName    # 1 test method
```

VSCode: `Ctrl+Shift+P` > "Tasks: Run Task" > chọn "Test: All Tests" hoặc "Test: Current File" (cấu hình trong `.vscode/tasks.json`).

### Test profile

Config test nằm tại `src/test/resources/application-test.yaml` (H2 in-memory, JWT test secret). **KHÔNG** đặt trong `src/main/resources/`.

### Test Directory Structure

```
src/test/java/com/priz/base/
├── BaseApplicationTests.java                          # Context load (@ActiveProfiles("test"))
├── testutil/
│   ├── TestFixtures.java                              # Factory methods & constants cho test data
│   └── SecurityTestUtil.java                          # Setup/clear SecurityContext cho service tests
├── infrastructure/security/
│   ├── SecurityContextHolderTest.java                 # Pure JUnit 5
│   └── aspect/SecuredAspectTest.java                  # Mock RequestContextHolder + JoinPoint
├── config/security/
│   └── JwtServiceTest.java                            # ReflectionTestUtils cho @Value fields
├── application/features/
│   ├── auth/impl/AuthServiceImplTest.java             # Mock repos, JwtService, PasswordEncoder
│   ├── users/impl/UserServiceImplTest.java            # SecurityTestUtil @BeforeEach/@AfterEach
│   └── files/impl/FileServiceImplTest.java            # Mock FileRepository, LocalStorageService
├── application/admin/internal/impl/
│   └── AdminServiceImplTest.java                      # Mock registry, introspector, spec builder
├── common/
│   ├── exception/GlobalExceptionHandlerTest.java      # Instantiate trực tiếp, test handler methods
│   └── util/
│       ├── DateTimeUtilTest.java
│       └── JsonUtilTest.java
└── interfaces/rest/
    ├── HealthControllerTest.java                      # @WebMvcTest
    ├── AuthControllerTest.java
    ├── UserControllerTest.java
    ├── FileControllerTest.java
    └── AdminControllerTest.java
```

### Quy tắc đặt tên File & Package

**Nguyên tắc: Cùng package, khác thư mục gốc.**

- Nếu class chính nằm ở `src/main/java/com/priz/base/application/features/auth/impl/AuthServiceImpl.java`, thì test **bắt buộc** nằm ở `src/test/java/com/priz/base/application/features/auth/impl/AuthServiceImplTest.java`.
- **Hậu tố `Test`** cho Unit Test, **hậu tố `IT`** cho Integration Test:
  - `AuthServiceImplTest.java` — chạy nhanh, dùng Mockito, không load context
  - `AuthServiceImplIT.java` — chạy chậm, load Spring Context, DB thật hoặc Testcontainers

### Quy tắc đặt tên hàm Test

Dùng phong cách **Unit-of-Work** (tương tự Spring Core):

```
[MethodName]_[StateUnderTest]_[ExpectedBehavior]
```

Ví dụ:
- `createNews_nullTitle_throwsIllegalArgumentException()`
- `register_duplicateEmail_throwsConflictException()`
- `login_validCredentials_returnsAuthResponse()`
- `download_forbiddenUser_throwsForbiddenException()`

Tên hàm phải đọc như một **đặc tả nghiệp vụ** — bất kỳ dev nào nhìn vào cũng hiểu hệ thống hoạt động thế nào.

### Cấu trúc bên trong hàm Test — AAA Pattern

Mọi hàm test **BẮT BUỘC** tuân theo cấu trúc **Arrange-Act-Assert**, phân cách bằng comment:

```java
@Test
void createNews_validData_returnsSavedEntity() {
    // Arrange
    NewsEntity newsToSave = NewsTestData.builder().withDefaultValues().build();
    when(newsRepository.save(any())).thenReturn(newsToSave);

    // Act
    NewsEntity result = newsService.createNews(newsToSave);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getTitle()).isEqualTo(newsToSave.getTitle());
    verify(newsRepository).save(any());
}
```

- **Arrange (Given):** Thiết lập dữ liệu mẫu, cấu hình Mock.
- **Act (When):** Gọi **duy nhất 1 hàm** cần kiểm thử.
- **Assert (Then):** Kiểm tra kết quả (`assertThat`) và hành vi (`verify`).

### Tổ chức phân cấp với @Nested

Khi một class có nhiều method cần test, dùng `@Nested` + `@DisplayName` để nhóm theo method/kịch bản — tránh file test dài hàng nghìn dòng:

```java
class AuthServiceImplTest {

    @Nested
    @DisplayName("register")
    class RegisterTests {
        @Test
        void validData_returnsAuthResponse() { ... }

        @Test
        void duplicateEmail_throwsConflictException() { ... }
    }

    @Nested
    @DisplayName("login")
    class LoginTests {
        @Test
        void validCredentials_returnsTokens() { ... }

        @Test
        void wrongPassword_throwsUnauthorized() { ... }
    }
}
```

### Test Data — Object Mother & Test Data Builders

**KHÔNG** tạo dữ liệu mẫu trực tiếp trong hàm test (boilerplate). Dùng:

- **TestFixtures (Object Mother):** `TestFixtures.createUserModel()`, `TestFixtures.createRegisterRequest()` — factory methods tập trung trong `testutil/TestFixtures.java`
- **Test Data Builder (khi cần customize):** `UserTestData.builder().withEmail("custom@test.com").build()`
- **Constants:** `TestFixtures.TEST_USER_ID`, `TestFixtures.TEST_EMAIL` — không hardcode magic strings

### Assertion — Dùng AssertJ

Ưu tiên **AssertJ** (`assertThat`) thay vì JUnit assertions (`assertEquals`) vì Fluent API dễ đọc hơn:

```java
// ĐÚNG — AssertJ
assertThat(result).isNotNull();
assertThat(result.getEmail()).isEqualTo("test@example.com");
assertThat(list).hasSize(3).extracting("name").contains("Alice", "Bob");

// TRÁNH — JUnit assertions
assertEquals("test@example.com", result.getEmail());
assertNotNull(result);
```

### Mocking Strategy

| Quy tắc | Chi tiết |
|----------|----------|
| Chỉ mock external dependencies | Database (Repository), external API clients, message producers, storage services |
| **KHÔNG** mock nội bộ | DTO, Entity, Converter, utility classes — dùng instance thật |
| Service tests | `@ExtendWith(MockitoExtension.class)`, `@Mock` dependencies, `@InjectMocks` SUT |
| Controller tests | `@WebMvcTest(XxxController.class)`, `@MockitoBean` services, `MockMvc` |
| SecurityContext | `@BeforeEach` gọi `SecurityTestUtil.setSecurityContext(...)`, `@AfterEach` gọi `clearSecurityContext()` |

### Spring Boot 4.x Test Notes

- `@WebMvcTest` import từ `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest` (KHÔNG PHẢI `org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest`)
- `@MockitoBean` thay thế `@MockBean` cũ, import từ `org.springframework.test.context.bean.override.mockito.MockitoBean`
- `ObjectMapper` không tự inject trong `@WebMvcTest` slice — phải tạo `new ObjectMapper()` trực tiếp
