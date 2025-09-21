# CineHub Gateway Service

## Overview
API Gateway cho hệ thống microservices CineHub. Gateway chịu trách nhiệm:
- Route requests đến các service phù hợp
- Xác thực JWT tokens
- Gắn user info vào headers cho downstream services
- CORS handling

## Architecture

```
Frontend (localhost:5173)
    ↓
Gateway (localhost:8080)
    ↓
├── Auth Service (localhost:8081)
├── User Profile Service (localhost:8082)
└── Movie Service (localhost:8083)
```

## Routes

| Path | Service | Port | Protected |
|------|---------|------|-----------|
| `/api/auth/**` | Auth Service | 8081 | Partial* |
| `/api/profiles/**` | User Profile Service | 8082 | Yes |
| `/api/movies/**` | Movie Service | 8083 | Yes |

*Auth endpoints like `/signin`, `/signup`, `/refreshtoken` không cần authentication.

## Headers Added by Gateway

Khi request được authenticate thành công, gateway sẽ thêm các headers sau:

- `X-User-Id`: UUID của user
- `X-User-Email`: Email của user  
- `X-User-Role`: Role của user (USER, STAFF, ADMIN)
- `X-Authenticated`: "true"

## Configuration

### JWT Settings
```yaml
jwt:
  secret: "your-secret-key"
  expiration: 86400000 # 24 hours
```

### Service URLs
Update trong `application.yml` nếu services chạy ở port khác:
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: auth-service
          uri: http://localhost:8081
          # ...
```

## Usage

### Start Gateway
```bash
cd services/gateway-service
mvn spring-boot:run
```

### Frontend Integration
Frontend chỉ cần gọi gateway (port 8080) thay vì gọi trực tiếp các service:

```javascript
// Before (direct service calls)
fetch('http://localhost:8081/api/auth/signin', ...)
fetch('http://localhost:8082/api/profiles/123', ...)

// After (through gateway)
fetch('http://localhost:8080/api/auth/signin', ...)
fetch('http://localhost:8080/api/profiles/123', {
  headers: {
    'Authorization': `Bearer ${accessToken}`
  }
})
```

### Downstream Services
Services phía sau gateway có thể lấy user info từ headers:

```java
@GetMapping("/{userId}")
public ResponseEntity<UserProfile> getProfile(
    @PathVariable UUID userId,
    @RequestHeader("X-User-Id") UUID authenticatedUserId,
    @RequestHeader("X-User-Role") String userRole) {
    
    // Kiểm tra permission
    if (!userId.equals(authenticatedUserId) && !"ADMIN".equals(userRole)) {
        return ResponseEntity.status(403).build();
    }
    
    // Business logic...
}
```

## Development

### Adding New Services
1. Thêm route mới trong `application.yml`
2. Cấu hình authentication filter nếu cần
3. Update documentation

### Testing
```bash
# Test authentication
curl -H "Authorization: Bearer <token>" http://localhost:8080/api/profiles/123

# Test CORS
curl -H "Origin: http://localhost:5173" http://localhost:8080/api/auth/signin
```