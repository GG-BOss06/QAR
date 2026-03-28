import requests
import json

# 测试数据
test_data = {
    "data": "这是一个测试数据，用于验证加密和解密功能",
    "policy": "role:admin"
}

# 加密测试
def test_encryption():
    print("=== 测试加密功能 ===")
    url = "http://localhost:8080/api/encrypt"
    response = requests.post(url, json=test_data)
    result = response.json()
    print(f"加密响应: {json.dumps(result, ensure_ascii=False, indent=2)}")
    return result

# 解密测试
def test_decryption(encrypted_data):
    print("\n=== 测试解密功能 ===")
    url = "http://localhost:8080/api/decrypt"
    decrypt_data = {
        "encryptedData": encrypted_data["encryptedData"],
        "policy": test_data["policy"]
    }
    response = requests.post(url, json=decrypt_data)
    result = response.json()
    print(f"解密响应: {json.dumps(result, ensure_ascii=False, indent=2)}")
    return result

if __name__ == "__main__":
    # 先测试加密
    encrypted_result = test_encryption()
    
    # 再测试解密
    if encrypted_result.get("code") == 200:
        decrypted_result = test_decryption(encrypted_result)
        
        # 验证解密结果
        if decrypted_result.get("code") == 200:
            original_data = test_data["data"]
            decrypted_data = decrypted_result.get("decryptedData", "")
            print(f"\n=== 验证结果 ===")
            print(f"原始数据: {original_data}")
            print(f"解密数据: {decrypted_data}")
            print(f"是否匹配: {original_data == decrypted_data}")
        else:
            print("解密失败")
    else:
        print("加密失败")
