# Kiến Trúc Xác Thực & Phân Quyền Đa Ngôn Ngữ (Kong API Gateway)

Tài liệu này mô tả chi tiết phương án kiến trúc hệ thống xác thực của `priz-base` tích hợp vào Kong Gateway. Thiết kế này đạt tiêu chuẩn Enterprise, áp dụng cơ chế **Phân Quyền Nhị Phân (Bitmask Authorization)** kết hợp với **Protocol Buffers (Protobuf)** để đảm bảo tính đồng bộ tuyệt đối trên hệ sinh thái đa ngôn ngữ.

---

## 1. Bối Cảnh & Quyết Định Thiết Kế (Context & Design Decisions)

Trong kiến trúc Microservices có sử dụng API Gateway, việc truyền tải thông tin phân quyền (Authorization) từ Gateway xuống các service con thường gặp phải vấn đề "thắt cổ chai" về giới hạn dung lượng HTTP Header hoặc làm phình to JWT Payload. Để giải quyết triệt để vấn đề này, kiến trúc áp dụng hai quyết định thiết kế cốt lõi:

### Tại sao lại dùng Bitmask (Phân quyền nhị phân)?
Thông thường, một user có thể sở hữu hàng trăm quyền riêng lẻ (ví dụ: `["CREATE_POST", "READ_POST", "UPDATE_POST", "DELETE_POST"]`). Nếu truyền mảng chuỗi này qua JWT hoặc HTTP Header:
- **Nguy cơ lỗi:** Hầu hết các web server (Tomcat, Nginx) giới hạn Header ở mức 4KB - 8KB. Chuỗi quyền quá dài sẽ gây ra lỗi `431 Request Header Fields Too Large`.
- **Giải pháp Bitmask:** Bằng cách gán mỗi hành động (Action) vào một vị trí Bit (CREATE = bit 0, READ = bit 1, UPDATE = bit 2, DELETE = bit 3), toàn bộ 4 quyền trên được nén lại thành một con số nguyên duy nhất: `1 + 2 + 4 + 8 = 15`. 
- **Lợi ích:** Kích thước JWT/Header được thu nhỏ cực hạn. Ngoài ra, việc sử dụng toán tử bitwise (`&`) trong code để kiểm tra quyền mang lại tốc độ thực thi (Performance) cực nhanh trên mọi ngôn ngữ lập trình.

### Tại sao giữ nguyên chuỗi String cho Key thay vì Băm (Hash)?
Mặc dù việc sử dụng thuật toán Băm (ví dụ: Hash CRC32 biến `"base:article_manager"` thành `12345678`) có thể giúp thu gọn dung lượng thành mảng số học tuyệt đối, thiết kế này vẫn quyết định giữ lại chuỗi String gốc cho phần Key.
- **Lý do:** Đảm bảo tính **Human-readable (Dễ dàng Debug)**.
- Khi xảy ra lỗi phân quyền trên Production, lập trình viên chỉ cần soi log hoặc mở tab Network để nhìn vào Header: `X-User-Permissions: base:article_manager=5`. 
- Họ ngay lập tức biết User này đang mang quyền `5` (CREATE và UPDATE) ở tài nguyên `base:article_manager` mà không cần phải dùng công cụ giải mã (Decode) hay dò bảng Hash nào. Việc hy sinh một vài bytes dung lượng để đổi lấy Trải nghiệm Lập trình viên (Developer Experience) và khả năng bảo trì (Maintainability) là một sự đánh đổi chuẩn xác cho hệ thống Enterprise.

---

## 2. Cơ Chế Phân Quyền Nhị Phân (Bitmask) & Đồng Bộ Protobuf

Để loại bỏ chuỗi (String) gây phình to cấu trúc lưu trữ và tối ưu tốc độ kiểm tra, hệ thống sử dụng **Số nguyên (Bitmask)** cho các Hành động (Actions).

### A. Đồng Bộ Bằng Protocol Buffers (.proto)
Mọi quyền sẽ được định nghĩa tập trung bằng một **Enum trong file `.proto`**.
```protobuf
syntax = "proto3";
package priz.auth;

enum PermissionType {
  CREATE = 0;   // Bit 0 (1)
  READ = 1;     // Bit 1 (2)
  UPDATE = 2;   // Bit 2 (4)
  DELETE = 3;   // Bit 3 (8)
  SEARCH = 4;   // Bit 4 (16)
  // Thêm quyền custom ở đây
}
```
*Lợi ích:* File `.proto` này sẽ được biên dịch tự động ra mã nguồn Java, Python, Go cho các service, đảm bảo hệ thống có một Single Source of Truth duy nhất và không bao giờ bị lệch bit.

### B. Annotation Cấu Hình
Sử dụng mảng `permissions` và tham số `isGlobal` để tối ưu quản lý phân quyền ở cấp độ method:
`@Secured(permissions = {PermissionType.READ, PermissionType.UPDATE}, customKey = "article_manager", isGlobal = false)`

- **customKey**: Tên nhóm tài nguyên.
- **isGlobal**: 
    - Nếu `false` (mặc định): Hệ thống tự động thêm Prefix là Namespace/Service name vào trước `customKey` (ví dụ: `base:article_manager`) để tránh xung đột giữa các service.
    - Nếu `true`: Hệ thống thêm Prefix là `global` vào trước `customKey` (ví dụ: `global:report`), dùng cho các quyền dùng chung toàn hệ thống.
- **Implicit Grouping**: Nếu không có `customKey`, hệ thống tự động lấy tên Controller làm Key (kèm prefix nếu `isGlobal = false`).

### C. Định dạng Chuỗi Quyền (Permissions String)
- Khi User có quyền READ (1) và UPDATE (4) trên `"base:article_manager"`, Bitmask = `5`.
- Nếu User có thêm quyền READ (2) trên nhóm `"global:report"`, Bitmask = `2`.
- **Định dạng**: Các quyền được ghép nối thành một chuỗi duy nhất với cấu trúc `Key=Bitmask` phân tách bằng dấu phẩy:
  `"base:article_manager=5,global:report=2"`
- Payload JWT sẽ chỉ lưu một trường String ngắn gọn này:
  `{"perms": "base:article_manager=5,global:report=2"}`

---

## 3. Luồng Kiểm Tra Phân Quyền Tại Microservices

**Bước 1:** Kong Gateway đọc JWT, bóc tách claim `"perms"` và ép trực tiếp thành HTTP Header: 
`X-User-Permissions: base:article_manager=5,global:report=2`

**Bước 2:** Khi request vào Microservice (Python, Go, Java):
1. Tách chuỗi Header trên bằng dấu phẩy `,` và dấu bằng `=`. Dễ dàng build ra một Map trong RAM: `{"base:article_manager": 5, "global:report": 2}`.
2. Lấy tên API/Controller đang gọi (VD: `"base:article_manager"`), tìm trong Map để lấy ra con số Bitmask (`5`).
3. Dùng toán tử bitwise `&` kết hợp với Enum sinh ra từ `.proto` để kiểm tra. Ví dụ API đòi hỏi `PermissionType.READ` (1) và `PermissionType.UPDATE` (2), mask đòi hỏi là `1 | 4 = 5`.
   Lệnh kiểm tra: `if (user_bitmask & required_mask) == required_mask` => Cho phép qua!

---

## 4. Tự Động Hóa Auto-Discovery & UI Admin

1. **Auto-Discovery qua Java Reflection (Khi khởi chạy hệ thống):**
   Tính năng quét quyền sẽ được tích hợp thẳng vào mã nguồn Java.
   - Sử dụng `ApplicationRunner` (hoặc `CommandLineRunner`) của Spring Boot.
   - Khi Application khởi động (hoặc khi gọi qua một lệnh Command cụ thể), Spring sẽ dùng Java Reflection quét toàn bộ các hàm được đánh dấu `@Secured` trong Context.
   - Hệ thống tự động thu thập thông tin: API này thuộc nhóm nào (`customKey`), yêu cầu những quyền nào.
   - Đẩy (Upsert) dữ liệu vừa quét được vào Database phân quyền. 
   *(Đảm bảo độ chính xác tuyệt đối 100%, dễ dàng debug và vận hành).*
   
2. **Giao diện Admin (UI) & Backend API xử lý Matrix:**
   Frontend hoàn toàn **không cần biết về Bitmask**:
   - **Đọc quyền (GET):** Admin API query DB, dịch chuỗi `base:article_manager=5` thành object `{"base:article_manager": ["CREATE", "UPDATE"]}` trả về cho Frontend vẽ Ma Trận Phân Quyền (Matrix).
   - **Lưu quyền (POST):** Khi Admin tick/bỏ tick, Frontend gửi ngược object `{"base:article_manager": ["CREATE", "READ", "UPDATE"]}` về Backend.
   - **Xử lý Bitmask:** Backend dựa vào `.proto` để encode lại các chữ (CREATE, READ) ra Bitmask (`1+2+4=7`) rồi lưu chuỗi `base:article_manager=7` xuống DB.

---

## 5. Quản Lý Thu Hồi Quyền (Revocation Workflow)

Hệ thống áp dụng chiến lược **Short-lived Access Token + Refresh Token**:

- **Thời hạn Token:** Cấu hình Access Token có TTL ngắn (Ví dụ: 5 - 15 phút) tại `priz-base`.
- **Sự kiện đổi quyền:** 
  1. Khi Admin thay đổi quyền, `priz-base` huỷ toàn bộ Access Token và Refresh Token của user trong MySQL.
  2. Thời gian trễ tối đa bằng phần đời còn lại của Access Token (dưới 5-15p).
- **Ép buộc đăng nhập lại:**
  1. Khi Access Token hết hạn, Kong trả lỗi `401`.
  2. Frontend dùng Refresh Token xin token mới -> `priz-base` từ chối vì token đã bị revoke -> Frontend bắt buộc user Login lại.

---

## 6. Lộ Trình Triển Khai (Roadmap)

1. **Đồng Bộ Thể Chế (Protocol Buffers)**
   - Khởi tạo file `permissions.proto` tập trung định nghĩa Enum `PermissionType`.
   - Biên dịch `.proto` sang mã nguồn Java cho `priz-base`.

2. **Cấu trúc lại Logic `@Secured` & Token tại `priz-base`**
   - Cập nhật `@Secured` để sử dụng tham số `permissions` (mảng Enum) và `isGlobal` (boolean).
   - Cập nhật `AuthServiceImpl`: Khi login, nén quyền của User thành chuỗi `key=bitmask,key=bitmask` để nhét vào JWT Payload.

3. **Auto-Discovery & Admin API**
   - Viết lớp `PermissionScannerRunner` (chạy qua Java Reflection / Spring Context) để quét `@Secured` khi khởi động ứng dụng và lưu vào DB.
   - Xây dựng Admin API GET/POST để trả về và lưu Ma Trận Phân Quyền, kèm logic tự động Encode/Decode từ Chữ sang Bitmask.

4. **Điều chỉnh Microservices & `priz-base`**
   - Cập nhật `SecuredAspect.java` và `JwtAuthenticationFilter.java`: Đọc trực tiếp Header `X-User-Permissions`, split bằng dấu phẩy và dấu bằng.
   - Kiểm tra quyền bằng toán tử bitwise `&` dựa trên Enum từ Protobuf.

5. **Cấu hình Kong Gateway (`kong.yml`)**
   - Viết plugin Lua `pre-function` (hoặc cấu hình request-transformer/oidc) giải mã JWT, gán claim `"perms"` vào Header `X-User-Permissions`.
