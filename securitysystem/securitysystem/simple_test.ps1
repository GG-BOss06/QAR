# 简单的测试脚本
$body = @"
{
  "data": "{\"flightId\": \"QF1234\", \"timestamp\": \"2026-03-26T10:30:00Z\", \"aircraft\": \"B737-800\"}",
  "policy": "role:admin"
}
"@

try {
    $response = Invoke-WebRequest -Uri "http://localhost:8081/api/encrypt" -Method Post -Body $body -ContentType "application/json"
    Write-Host "Response:"
    Write-Host $response.Content
} catch {
    Write-Host "Error: $($_.Exception.Message)"
}