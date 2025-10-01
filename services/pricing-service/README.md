# Pricing Service

Spring Boot microservice for managing ticket pricing in CineHub.

## Features

- REST API for creating and listing pricing
- PostgreSQL database (config qua .env)
- Ready to run với Maven

## How to run

```bash
mvn spring-boot:run
```

## API Endpoints

- `POST /api/pricing` - Tạo pricing mới
- `GET /api/pricing` - Lấy danh sách pricing
- `GET /api/pricing/ping` - Health check

## Structure

- `controller/` - REST controllers
- `service/` - Business logic
- `entity/` - JPA entities
- `repository/` - Spring Data repositories
- `dto/` - Data transfer objects

## Configuration

Sửa `src/main/resources/application.properties` hoặc `.env` để cấu hình DB và port.
