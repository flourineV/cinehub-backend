@echo off
curl -X POST http://localhost:8080/api/auth/signup ^
  -H "Content-Type: application/json" ^
  -d "{\"fullName\":\"Nguyen Van Test 2\",\"dateOfBirth\":\"1995-03-20\",\"phoneNumber\":\"0123456789\",\"username\":\"testuser2\",\"nationalId\":\"987654321098\",\"email\":\"test2@example.com\",\"password\":\"password123\",\"confirmPassword\":\"password123\"}"
pause