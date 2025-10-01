# Booking Service

Spring Boot microservice for booking movie tickets in CineHub.

## Features

- REST API for creating and listing bookings
- H2 in-memory database for development
- Ready to run with Maven

## How to run

```bash
mvn spring-boot:run
```

## API Endpoints

- `POST /api/bookings` - Create a new booking
- `GET /api/bookings` - List all bookings
- `GET /api/bookings/ping` - Health check

## Structure

- `controller/` - REST controllers
- `service/` - Business logic
- `entity/` - JPA entities
- `repository/` - Spring Data repositories
- `dto/` - Data transfer objects

## Configuration

Edit `src/main/resources/application.properties` for DB and port settings.
