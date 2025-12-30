# Cinehub Backend

Backend của dự án Cinehub — tập hợp microservices được triển khai bằng Spring Boot.

Nội dung README này bao gồm: mô tả tổng quan, sơ đồ thư mục, danh sách services và port (ghi chú giá trị mặc định nếu có), hướng dẫn build/chạy cục bộ và bằng Docker, cấu hình biến môi trường, debug và các bước triển khai cơ bản.

**Tổng quan**

- Kiến trúc: microservices — mỗi service là một ứng dụng Spring Boot độc lập.
- Build: Maven (JDK 17)
- Container: Docker (Dockerfile cho từng service) và `docker-compose.yml` ở gốc repo để khởi nhiều service cùng lúc.

**Cấu trúc thư mục chính (tổng quát)**

```
./
├── docker-compose.yml
├── services/
│   ├── auth-service/
│   │   ├── Dockerfile
│   │   ├── pom.xml
│   │   └── src/main/resources/application.properties
	 ...
│   ├── booking-service/
│   ├── fnb-service/
│   ├── gateway-service/
│   ├── movie-service/
│   ├── notification-service/
│   ├── payment-service/
│   ├── pricing-service/
│   ├── promotion-service/
│   ├── review-service/
│   ├── showtime-service/
│   └── user-profile-service/
└── README.md
```

Trong mỗi service bạn sẽ thường thấy: `pom.xml`, `Dockerfile`, `src/main/java`, `src/main/resources/application.properties` (hoặc `.yaml`) và đôi khi `.env.example`.

**Các service & Port (tóm tắt và ghi chú)**

Lưu ý: nhiều service sử dụng biến môi trường `SERVER_PORT` trong `application.properties`. Một số service có giá trị mặc định đặt trong file properties, một số khác không có mặc định và cần được set thông qua `.env` hoặc biến môi trường khi chạy.

Quan sát từ repo hiện tại:

- `movie-service`: `server.port=${SERVER_PORT:8083}` → mặc định 8083 (cũng có `.env.example` với `SERVER_PORT=8081`, bạn có thể ghi đè)
- Các service khác (ví dụ `auth-service`, `booking-service`, `fnb-service`, `notification-service`, `payment-service`, `pricing-service`, `promotion-service`, `review-service`, `showtime-service`, `user-profile-service`) đều dùng `server.port=${SERVER_PORT}` (không có default trong file), nghĩa là bạn phải cung cấp `SERVER_PORT` khi chạy.

Ví dụ bảng port (KHÔNG phải là cấu hình bắt buộc — là mapping ví dụ để chạy đồng thời các service trên máy dev):

|              Service | Ví dụ port (gợi ý) | Ghi chú                           |
| -------------------: | -----------------: | :-------------------------------- |
|      gateway-service |               8080 | API Gateway (gợi ý)               |
|         auth-service |               8081 | set `SERVER_PORT=8081`            |
|      booking-service |               8082 | set `SERVER_PORT=8082`            |
|        movie-service |    8083 (mặc định) | `server.port=${SERVER_PORT:8083}` |
|     showtime-service |               8084 | set `SERVER_PORT=8084`            |
| user-profile-service |               8085 | set `SERVER_PORT=8085`            |
| notification-service |               8086 | set `SERVER_PORT=8086`            |
|      payment-service |               8087 | set `SERVER_PORT=8087`            |
|      pricing-service |               8088 | set `SERVER_PORT=8088`            |
|    promotion-service |               8089 | set `SERVER_PORT=8089`            |
|       review-service |               8090 | set `SERVER_PORT=8090`            |
|          fnb-service |               8091 | set `SERVER_PORT=8091`            |

Thực tế: bạn có thể chọn port khác; chỉ cần đảm bảo mỗi service chạy trên port riêng để tránh xung đột.

**Yêu cầu trước khi chạy**

- Java 17 (JDK 17)
- Maven 3.6+
- Docker & Docker Compose (nếu chạy containerized)

**Cách run & build (chi tiết)**

1. Chạy từng service (cục bộ, không Docker)

- Bước 1: vào thư mục service, ví dụ `services/movie-service`:

```powershell
cd services\movie-service
```

- Bước 2: export hoặc set biến môi trường `SERVER_PORT` (Windows PowerShell ví dụ):

```powershell
$env:SERVER_PORT = "8083"
# hoặc trong cmd: set SERVER_PORT=8083
```

- Bước 3: chạy bằng Maven:

```powershell
mvn clean spring-boot:run
# hoặc build jar rồi chạy
mvn clean package
java -jar target\*.jar
```

2. Chạy toàn bộ bằng Docker Compose (khuyến nghị cho dev/full stack)

- Chuẩn bị: cập nhật `docker-compose.yml` để export biến `SERVER_PORT` cho từng service hoặc tạo file `.env` ở gốc repo với biến được dùng trong `docker-compose.yml`.

Ví dụ file `.env` gợi ý (gốc repo):

```
# Example .env for docker-compose (gợi ý)
GATEWAY_PORT=8080
AUTH_PORT=8081
BOOKING_PORT=8082
MOVIE_PORT=8083
SHOWTIME_PORT=8084
USER_PROFILE_PORT=8085
NOTIFICATION_PORT=8086
PAYMENT_PORT=8087
PRICING_PORT=8088
PROMOTION_PORT=8089
REVIEW_PORT=8090
FNB_PORT=8091
```

Và trong `docker-compose.yml` bạn map biến vào `environment:` từng service, ví dụ:

```yaml
services:
	movie-service:
		build: ./services/movie-service
		environment:
			- SERVER_PORT=${MOVIE_PORT}
		ports:
			- "${MOVIE_PORT}:${MOVIE_PORT}"
```

- Khởi lên:

```bash
docker-compose up --build
# hoặc nền
docker-compose up -d --build
```

3. Chạy unit/integration tests

- Trong thư mục service:

```bash
mvn test
```

**Cấu hình chi tiết / biến môi trường**

- Mỗi service có thể cần các biến như: `SERVER_PORT`, database URL/username/password, thông tin message broker, API keys, v.v. Kiểm tra `src/main/resources/application.properties` và file `.env.example` trong từng service.
- Thay đổi bằng cách:
  - Sửa trực tiếp `src/main/resources/application.properties` (không khuyến nghị cho production), hoặc
  - Thiết lập biến môi trường trước khi chạy, hoặc
  - Cấu hình trong `docker-compose.yml`/`.env` khi dùng Docker.

Ví dụ `src/main/resources/application.properties` mẫu dùng placeholder:

```
server.port=${SERVER_PORT}
spring.application.name=movie-service
# database, kafka, etc.
```

**Health check & kiểm tra nhanh**

- Nếu Spring Actuator được bật (thường thêm dependency `spring-boot-starter-actuator`), bạn có thể kiểm tra `/actuator/health`:

```bash
curl http://localhost:8083/actuator/health
```

- Kiểm tra endpoints API bằng curl/Postman theo port dịch vụ tương ứng.

**Debug / Troubleshooting**

- Lỗi port conflict: đảm bảo `SERVER_PORT` không trùng giữa các service.
- Thiếu biến môi trường: kiểm tra log startup, Spring sẽ báo lỗi thiếu property nếu cần.
- Lỗi Maven build: chạy `mvn -X` để có log chi tiết.
- Container không khởi: kiểm tra `docker logs <container>`.

**Phát triển & đóng góp**

- Mỗi service riêng biệt — tách pull request theo service sẽ dễ review hơn.
- Giữ consistent Java version (JDK 17) và cấu hình Maven plugin giống nhau giữa các service.
- Nếu thêm service mới: copy template từ một service hiện có, thêm `Dockerfile` và update `docker-compose.yml`.

**Triển khai (basic)**

- Production thường deploy từng service dưới dạng container (k8s/ECS) hoặc VM. Thiết lập env vars cho production và secrets quản lý qua vault/secret manager.

**Tài liệu thêm**

- Mỗi service có thể có README riêng (ví dụ [services/pricing-service/README.md](services/pricing-service/README.md)). Kiểm tra README nội bộ để biết cấu hình DB chi tiết hoặc migration scripts.

---

Ghi chú: README này dựa trên cấu trúc và các file config tìm thấy trong repo. Nếu bạn muốn tôi liệt kê port chính xác hiện đang sử dụng trong môi trường `docker-compose.yml` hoặc cập nhật `docker-compose.yml` với mapping port thực tế, cho tôi quyền/cho biết muốn dùng port nào, tôi sẽ chỉnh file giúp.
