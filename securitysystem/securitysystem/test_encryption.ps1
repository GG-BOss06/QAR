# 读取QAR示例数据
$qarData = Get-Content -Path ".\qar_sample_data.json" -Raw

# 构建请求体 - 确保数据正确转义
$requestBody = @{
    data = $qarData
    policy = "role:admin AND department:security"
}

# 转换为JSON，确保特殊字符正确转义
$jsonBody = $requestBody | ConvertTo-Json -Depth 10

# 测试加密端点
Write-Host "Testing encryption endpoint..."
Write-Host "Sending QAR data for encryption..."

try {
    $response = Invoke-RestMethod -Uri "http://localhost:8081/api/encrypt" -Method Post -Body $jsonBody -ContentType "application/json"
    
    Write-Host "\n=== Encryption Result ==="
    Write-Host "Status Code: $($response.code)"
    Write-Host "Message: $($response.message)"
    Write-Host "\nEncrypted Data (first 100 chars): $($response.encryptedData.Substring(0, [Math]::Min(100, $response.encryptedData.Length)))..."
    Write-Host "Wrapped Key: $($response.wrappedKey)"
    Write-Host "========================"
    
} catch {
    Write-Host "Error: $($_.Exception.Message)"
    Write-Host "Response: $($_.Exception.Response)"
}