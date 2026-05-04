# CLAUDE.md

Hướng dẫn cho Claude Code khi làm việc với repository này. Đây là tài liệu kiến trúc tổng thể — mọi phát triển tính năng PHẢI tuân theo.

## Project Overview

Spring Boot 4.0.5 REST API & gRPC project (`com.priz.base`). Java 21, Maven, MySQL (`priz_base`), Elasticsearch, gRPC, Lombok, Spring Data JPA/Elasticsearch, Spring Security (stateless JWT), Spring Actuator, SpringDoc OpenAPI.

Hệ thống tích hợp **ELK Stack** (Filebeat -> Logstash -> Elasticsearch) cho log shipping và search. Tích hợp **MCP (Model Context Protocol)** để cung cấp công cụ cho các AI Agents.

Phụ thuộc vào module `com.priz:common` (shared library) và `com.priz:interfaces` (gRPC stubs).

## Build & Run

```bash
mvnw.cmd spring-boot:run                    # Chạy app (REST: 8083, gRPC: 9091)
mvnw.cmd compile                             # Compile
mvnw.cmd test                                # Chạy tất cả test
mvnw.cmd test -Dtest=ClassName               # Chạy 1 test class
mvnw.cmd test -Dtest=ClassName#methodName    # Chạy 1 test method
mvnw.cmd package                             # Build JAR
```

> **Lưu ý:** `common` và `interfaces` module phải được install trước khi build `base`:
> ```bash
> cd ../common && mvnw.cmd install
> cd ../interfaces && mvnw.cmd install
> ```

---

## Architecture: Hexagonal (Ports & Adapters)

Base package: `com.priz.base`

### Layer Map — Nơi tìm kiếm & nơi đặt code

| Layer | Package | Mục đích | Chứa gì |
|-------|---------|----------|---------|
| **Common** | `common/` | Shared kernel (base-local) | JPA `@MappedSuperclass` base (`model/BaseModel`), `GlobalExceptionHandler`, API response wrappers, utility classes, AOP logging. |
| **Config** | `config/` | Cấu hình framework | Spring Security, JPA/ES auditing, gRPC config, OpenAPI config. |
| **Infrastructure** | `infrastructure/` | Outbound adapters | `SecuredAspect` AOP, messaging producers, low-level external clients. |
| **Domain** | `domain/` | Physical data layer | **mysql/**: JPA entities & repos. **elasticsearch/**: ES documents & repos. |
| **Application** | `application/` | Business logic core | **features/**: Business modules. **integration/**: High-level clients (GitHub, Telegram). **mcp/**: MCP Tool providers & dispatchers. **admin/**: Admin logic. |
| **Interfaces** | `interfaces/` | Inbound adapters | **rest/**: REST controllers. **grpc/**: gRPC services. **sqs/**: SQS consumers. **worker/**: Scheduled workers. |
| **ELK Stack** | `elk/` | Log shipping | `filebeat/`: Collector. `logstash/`: Pipeline & templates. |

### Directory Tree

```
src/main/java/com/priz/base/
├── BaseApplication.java                     # scanBasePackages: com.priz.base + com.priz.common
│
├── common/                              # BASE-LOCAL SHARED KERNEL
│
├── config/                              # FRAMEWORK CONFIG
│   ├── database/                        # Jpa & Elasticsearch Auditing
│   ├── security/                        # SecurityConfig
│   └── grpc/                            # gRPC Server/Client Config
│
├── infrastructure/                      # OUTBOUND ADAPTERS
│
├── domain/                              # PHYSICAL DATA LAYER
│   ├── mysql/                           # MySQL Entities & Repositories
│   └── elasticsearch/                   # ES Documents & Repositories
│
├── application/                         # BUSINESS LOGIC
│   ├── features/                        # Business Modules (auth, users, files, etc.)
│   ├── integration/                     # External Integrations (github, telegram)
│   ├── mcp/                             # Model Context Protocol (AI Tools)
│   └── admin/                           # Generic Admin CRUD system
│
└── interfaces/                          # INBOUND ADAPTERS
    ├── rest/                            # REST Controllers (Port 8083)
    └── grpc/                            # gRPC Services (Port 9091)
```

---

## Strict Rules — PHẢI tuân theo

1. **REST Controllers** nằm trong `interfaces/rest/`. **gRPC Services** nằm trong `interfaces/grpc/`.
2. **JPA Entities** nằm trong `domain/mysql/priz_base/model/`. **ES Documents** nằm trong `domain/elasticsearch/model/`.
3. **External Integrations** (GitHub, Telegram, etc.) PHẢI đặt trong `application/integration/`.
4. **MCP Tools** (dành cho AI) PHẢI đặt trong `application/mcp/provider/`.
5. **Business logic** nằm trong `application/features/{feature}/impl/`.
6. **Logging**: PHẢI sử dụng structured JSON logging. Logs ghi ra `logs/app.json` để Filebeat thu thập.
7. **Tất cả API response** dùng `ApiResponse<T>` wrapper.
8. **Protected endpoints** dùng `@Secured` (custom AOP).
9. **gRPC stubs**: Sử dụng từ module `com.priz:interfaces`.
10. **TUYỆT ĐỐI KHÔNG** dùng inline import. Khai báo `import` ở đầu file.
11. **Exceptions**: Import từ `com.priz.common.exception.*`.

---

## Hướng dẫn thêm Feature Module mới

1. Tạo package: `application/features/{feature_name}/`
2. Tạo service interface: `{Feature}Service.java`
3. Tạo `impl/{Feature}ServiceImpl.java` với `@Service`
4. Tạo `dto/`, `converter/`, `helper/` nếu cần.
5. Tạo controller trong `interfaces/rest/` hoặc service trong `interfaces/grpc/`.

---

## Admin Generic CRUD System

Hệ thống admin tự động CRUD cho mọi JPA entity gắn `@AdminManaged`.

### Expose entity vào admin API
- Gắn `@AdminManaged` lên entity class.
- Gắn `@AdminHidden` lên các field nhạy cảm.
- Repository phải extends `JpaSpecificationExecutor`.

---

## Security Model

- **JWT access tokens** (1 giờ) + **refresh tokens** (7 ngày, lưu trong DB).
- Authorization qua AOP `@Secured` annotation.
- User context: `SecurityContextHolder.getCurrentUserId()`.

## Naming Conventions

| Loại | Pattern | Ví dụ |
|------|---------|-------|
| JPA Entity | `{Name}Model.java` | `UserModel` |
| ES Document | `{Name}Document.java` | `LogDocument` |
| Repository | `{Name}Repository.java` | `UserRepository` |
| Service | `{Name}Service.java` / `ServiceImpl.java` | `AuthServiceImpl` |
| gRPC Service | `{Name}GrpcService.java` | `UserGrpcService` |
| Controller | `{Name}Controller.java` | `AuthController` |

## Tech Stack

- **Spring Boot 4.0.5**, Java 21, Maven.
- **gRPC** (1.77.1) - Port 9091.
- **Elasticsearch** - Search & Logging storage.
- **Filebeat & Logstash** - Log shipping pipeline.
- **MySQL** - Primary relational DB.
- **Lombok**, **SpringDoc OpenAPI** (2.8.6), **AssertJ**.

---

## Testing Rules

1. **AAA Pattern**: Arrange - Act - Assert.
2. **AssertJ**: Ưu tiên `assertThat(result).isEqualTo(expected)`.
3. **Mocking**: Dùng `@MockitoBean` cho Spring Boot 4.x.
4. **Naming**: `[MethodName]_[StateUnderTest]_[ExpectedBehavior]`.
5. **Location**: Cùng package với class chính, nhưng nằm trong `src/test/java`.

## API & Ports

- **REST API**: `8083` (Context Path: `/`)
- **gRPC Server**: `9091`
- **Swagger UI**: `/swagger-ui.html`
- **Actuator**: `/actuator/health`
- **Logs**: `logs/app.json` (Structured JSON)
