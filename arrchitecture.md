Để hiểu rõ tại sao project của bạn lại được tổ chức như vậy, chúng ta sẽ đi sâu vào từng lớp kiến trúc, các kỹ thuật bổ trợ và những Design Pattern (mẫu thiết kế) cốt lõi đang vận hành bên dưới.

---

## 1. Kiến trúc tổng thể: Hexagonal Architecture (Kiến trúc Lục giác)

Đây là "xương sống" của project. Thay vì chia tầng kiểu truyền thống (Layered Architecture) dễ bị phụ thuộc vào Database, Hexagonal chia hệ thống thành **Lõi (Core)** và các **Adapter**.

* **Lõi (Application/Features):** Nơi chứa toàn bộ Business Logic. Nó không biết gì về thế giới bên ngoài (không biết mình đang dùng Postgres hay MySQL, không biết request đến từ REST hay gRPC).
* **Inbound Adapters (Interfaces):** Các "cửa ngõ" để tác động vào lõi. Bao gồm `rest`, `grpc`, `worker`, `sqs/consumer`.
* **Outbound Adapters (Infrastructure & Domain):** Các "công cụ" mà lõi dùng để đẩy dữ liệu ra ngoài. Bao gồm `persistence` (Database), `messaging/sqs/producer` (Gửi tin), `external` (Gọi API đối tác).



---

## 2. Các Design Pattern (Mẫu thiết kế) chủ đạo

Project của bạn sử dụng kết hợp nhiều Pattern để giải quyết các vấn đề cụ thể:

### A. Facade Pattern (tại `application/features`)
Các `NewsService` hay `ProductService` đóng vai trò là **Facade**. 
* **Cách hoạt động:** Nó cung cấp một giao diện đơn giản cho các `interfaces` (Controller/Worker) gọi vào. Bên trong, nó có thể điều phối rất nhiều logic phức tạp: gọi Converter, gọi nhiều Repository khác nhau, gọi Producer gửi tin.
* **Lợi ích:** Giúp các lớp bên ngoài không cần quan tâm đến sự phức tạp bên trong lõi.

### B. Adapter Pattern (tại `interfaces` và `infrastructure`)
* **Inbound Adapter:** Chuyển đổi dữ liệu từ giao thức (HTTP JSON, gRPC Protobuf, SQS Message) sang dạng mà lõi hiểu được (**DTO**).
* **Outbound Adapter:** Chuyển đổi yêu cầu từ lõi sang lệnh cụ thể cho hạ tầng (SQL cho Postgres, BSON cho MongoDB, SDK AWS cho SQS).

### C. Strategy Pattern (tại `domain`)
Được áp dụng khi xử lý **Multi-DB**. 
* **Cách hoạt động:** Hệ thống có nhiều "chiến lược" lưu trữ khác nhau (Postgres, MySQL, Mongo). Tùy vào yêu cầu nghiệp vụ, hệ thống sẽ chọn đúng Repository và TransactionManager tương ứng để thực thi.

### D. Proxy Pattern & AOP (tại `infrastructure/security`)
Sử dụng **Aspect-Oriented Programming (AOP)** để xử lý các vấn đề cắt ngang (Cross-cutting concerns).
* **Kỹ thuật:** Khi bạn đánh dấu `@Secured`, một Proxy sẽ được tạo ra để "chặn" method đó lại, kiểm tra quyền hạn trước khi cho phép code nghiệp vụ chạy. Điều này giúp code nghiệp vụ luôn "sạch", không bị lẫn logic check quyền.

### E. Data Transfer Object (DTO) & Data Mapper
* **DTO Pattern:** Sử dụng các class trong thư mục `dto` để vận chuyển dữ liệu, tránh việc lộ cấu trúc Database ra ngoài.
* **Mapper/Converter:** Sử dụng MapStruct hoặc các class `converter` để chuyển đổi qua lại giữa **DTO $\leftrightarrow$ Entity $\leftrightarrow$ Model**.

---

## 3. Các kỹ thuật đặc biệt (Advanced Techniques)

### Separation of Concerns (Tách biệt mối quan tâm)
Đây là lý do bạn có 3 loại class dữ liệu khác nhau:
1.  **Request/Response (DTO):** Định dạng để nói chuyện với khách hàng.
2.  **Entity (Application):** Định dạng để tư duy nghiệp vụ.
3.  **Model (Domain):** Định dạng để nói chuyện với Database.
> *Kỹ thuật này giúp bạn có thể đổi tên cột trong DB mà không làm ảnh hưởng đến Frontend, hoặc gộp 2 bảng DB thành 1 đối tượng nghiệp vụ duy nhất.*

### Inversion of Control (IoC) & Dependency Injection (DI)
Spring Boot quản lý vòng đời của các class. 
* **Interface-based:** Bạn khai báo `NewsService` là một Interface. Tầng `interfaces` chỉ biết đến Interface này. 
* **Lợi ích:** Bạn có thể dễ dàng thay đổi bản thực thi (`impl`) hoặc Mock dữ liệu khi viết Unit Test mà không cần sửa code ở tầng `interfaces`.

### Event-Driven Messaging (SQS)
Kỹ thuật xử lý bất đồng bộ (Asynchronous):
* **Producer:** Đẩy tin nhắn đi và kết thúc request ngay lập tức để giải phóng tài nguyên.
* **Consumer:** Xử lý tin nhắn ở một luồng khác. 
* **Tính chất:** Giúp hệ thống chịu tải tốt hơn (High Availability) và cô lập lỗi (nếu Consumer lỗi, nó sẽ thử lại sau mà không làm treo Web API).

---

## 4. Tóm tắt luồng vận hành kỹ thuật



1.  **Request Layer:** Nhận dữ liệu (JSON) $\rightarrow$ Validate $\rightarrow$ Map vào **DTO**.
2.  **Security Layer (AOP):** Chặn Proxy $\rightarrow$ Giải mã JWT $\rightarrow$ Kiểm tra quyền.
3.  **Application Layer:** Nhận DTO $\rightarrow$ Chuyển thành **Entity** $\rightarrow$ Thực thi logic nghiệp vụ (tính toán, kiểm tra điều kiện).
4.  **Domain Layer:** Chuyển Entity thành **Model** $\rightarrow$ Thực thi lưu trữ xuống đúng DB vật lý thông qua **Repository**.
5.  **Messaging Layer:** Kích hoạt **Producer** gửi sự kiện ra **SQS**.
6.  **Background Processing:** **Consumer** nhận sự kiện $\rightarrow$ Gọi Service để hoàn tất các tác vụ phụ (Worker cũng hoạt động tương tự theo lịch).

**Tổng kết:** Project của bạn không chỉ là các folder, mà là một cỗ máy được thiết kế để **"Dễ thay đổi"** và **"Khó đổ vỡ"**. Mỗi lớp đóng vai trò như một bức tường ngăn cách để lỗi không lan rộng và sự thay đổi ở chỗ này không kéo theo sự sụp đổ ở chỗ khác.