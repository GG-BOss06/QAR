import json
import requests

# 读取QAR示例数据
with open('qar_sample_data.json', 'r', encoding='utf-8') as f:
    qar_data = f.read()

# 构建请求数据
payload = {
    "data": qar_data,
    "policy": "role:admin AND department:security"
}

# 发送请求
try:
    response = requests.post('http://localhost:8081/api/encrypt', json=payload)
    response.raise_for_status()  # 检查状态码
    
    result = response.json()
    print("=== Encryption Result ===")
    print(f"Status Code: {result['code']}")
    print(f"Message: {result['message']}")
    print(f"\nEncrypted Data (first 100 chars): {result['encryptedData'][:100]}...")
    print(f"Wrapped Key: {result['wrappedKey']}")
    print("========================")
    
except Exception as e:
    print(f"Error: {e}")
    if hasattr(e, 'response') and e.response:
        print(f"Response status: {e.response.status_code}")
        print(f"Response content: {e.response.text}")