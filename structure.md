src/main/java/com/yourcompany/project
├── common/                          # 0. SHARED KERNEL (Tiện ích hệ thống)
│   ├── logger/                      # AOP Logging, TraceId, Custom Log Appender
│   ├── storage/                     # Service upload (S3, Minio, Local)
│   ├── exception/                   # Global Exception Handler & Error Codes
│   └── util/                        # Helper: Date, String, Json, Reflection
│
├── config/                          # 1. SYSTEM CONFIG (Cấu hình Framework)
│   ├── security/                    # Spring Security Filter Chain, JWT Config
│   ├── database/                    # Routing DataSource cho Postgres, MySQL, Mongo
│   ├── rpc/                         # gRPC Server & Client Stub configuration
│   ├── aws/                         # AWS SDK Config (SqsClient Bean)
│   └── websocket/                   # WebSocket Broker & Endpoint config
│
├── infrastructure/                  # 2. OUTBOUND ADAPTERS (Thực thi kỹ thuật)
│   ├── security/                    # Logic xử lý Custom Annotations (AOP)
│   │   ├── annotation/              # @Secured, @HasAuthorityApiKey
│   │   └── aspect/                  # Aspect kiểm tra Quyền/Token
│   ├── messaging/                   # Tầng gửi tin nhắn (Producers)
│   │   └── sqs/                     # Gửi tin cho từng nghiệp vụ (Outbound)
│   │       ├── NewsSqsProducer.java   
│   │       └── ProductSqsProducer.java
│   └── external/                    # Gọi ra ngoài (Webhook call, gRPC Client)
│
├── domain/                          # 3. PHYSICAL DATA LAYER (Dữ liệu vật lý)
│   ├── postgresql/                  # Nhóm server PostgreSQL
│   │   └── user_db/                 # Database cụ thể
│   │       ├── model/               # @Entity JPA (Ánh xạ trực tiếp bảng DB)
│   │       ├── repository/          # Spring Data Interfaces
│   │       └── converter/           # JPA AttributeConverter (Enum <-> DB Column)
│   ├── mysql/                       # Nhóm server MySQL
│   │   └── product_db/              
│   │       ├── model/               
│   │       ├── repository/          
│   │       └── converter/           
│   └── mongodb/                     # Nhóm server NoSQL
│       └── analytics_db/            
│           ├── model/               # @Document
│           ├── repository/          
│           └── converter/           
│
├── application/                     # 4. LOGICAL CORE (Lõi nghiệp vụ)
│   ├── features/                    # --- NHÓM MODULES NGHIỆP VỤ ---
│   │   ├── news/                    # Module News
│   │   │   ├── NewsService.java     # Interface (Hợp đồng nghiệp vụ)
│   │   │   ├── impl/                # NewsServiceImpl (Thực thi logic)
│   │   │   ├── dto/                 # Request & Response (Object giao tiếp API)
│   │   │   ├── entity/              # Business Entity (Đối tượng xử lý logic nội bộ)
│   │   │   ├── converter/           # Mapper (DTO <-> Entity <-> Model)
│   │   │   ├── helper/              # Hàm xử lý dữ liệu đặc thù
│   │   │   └── constant/            # Enum/Constant nội bộ module
│   │   └── product/                 # Module Product
│   │       └── ... (Tương tự cấu trúc News)
│   │
│   ├── admin/                       # --- LOGIC QUẢN TRỊ ---
│   │   ├── internal/                # Quản lý trực tiếp các Model DB vật lý (CRUD)
│   │   └── feature/                 # Logic quản trị tùy chỉnh, dashboard
│   │
│   └── constant/                    # --- GLOBAL CONSTANTS ---
│       ├── CacheKey.java            # Prefix key cho Redis
│       ├── ConfigKey.java           # Key cấu hình hệ thống
│       └── LogKey.java              # Key định danh cho Logging & Tracing
│
└── interfaces/                      # 5. INBOUND ADAPTERS (Cổng giao tiếp đầu vào)
    ├── rest/                        # REST Controllers (API & Admin)
    ├── grpc/                        # Thực thi gRPC Services (Server-side)
    ├── socket/                      # WebSocket Handlers (Listen & Push)
    ├── sqs/                         # Xử lý tin nhắn đến (Consumers)
    │   ├── NewsSqsConsumer.java     # Nhận tin và gọi Service tương ứng xử lý
    │   └── ProductSqsConsumer.java
    ├── worker/                      # Các tác vụ chạy ngầm (@Scheduled)
    │   ├── NewsWorker.java          # Kích hoạt task định kỳ
    │   └── ProductWorker.java
    └── webhook/                     # Endpoints tiếp nhận Webhook bên ngoài