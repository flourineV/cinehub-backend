$body = @{
    fullName = "Nguyen Van Test 2"
    dateOfBirth = "1995-03-20"
    phoneNumber = "0123456789"
    username = "testuser2"
    nationalId = "987654321098"
    email = "test2@example.com"
    password = "password123"
    confirmPassword = "password123"
} | ConvertTo-Json

$headers = @{
    "Content-Type" = "application/json"
}

try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/signup" -Method POST -Body $body -Headers $headers
    Write-Host "✅ Signup Success!" -ForegroundColor Green
    $response | ConvertTo-Json -Depth 10
} catch {
    Write-Host "❌ Signup Failed!" -ForegroundColor Red
    Write-Host $_.Exception.Message
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $responseBody = $reader.ReadToEnd()
        Write-Host "Response: $responseBody"
    }
}