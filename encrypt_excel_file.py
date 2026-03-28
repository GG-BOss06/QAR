import requests
import json

# Excel文件路径
excel_file_path = 'QAR示例数据.xlsx'

# 准备文件上传
files = {
    'file': open(excel_file_path, 'rb')
}

# 准备其他参数
data = {
    'policy': 'role:admin AND department:security'
}

print("正在上传并加密Excel文件...")
print(f"文件路径: {excel_file_path}")

try:
    # 发送POST请求
    response = requests.post('http://localhost:8080/api/encrypt-excel', files=files, data=data)
    response.raise_for_status()
    
    result = response.json()
    
    print("\n" + "="*50)
    print("加密结果")
    print("="*50)
    print(f"状态码: {result['code']}")
    print(f"消息: {result['message']}")
    print(f"原始文件名: {result['originalFileName']}")
    print(f"文件大小: {result['fileSize']} bytes")
    print(f"\n原始数据预览 (前500字符):")
    print(result['preview'])
    print(f"\n加密数据 (前200字符): {result['encryptedData'][:200]}...")
    print(f"封装密钥: {result['wrappedKey']}")
    print("="*50)
    
    # 保存加密结果到文件
    with open('encrypted_qar_excel_result.json', 'w', encoding='utf-8') as f:
        json.dump(result, f, ensure_ascii=False, indent=2)
    print("\n加密结果已保存到: encrypted_qar_excel_result.json")
    
except Exception as e:
    print(f"错误: {e}")
    if hasattr(e, 'response') and e.response:
        print(f"响应状态码: {e.response.status_code}")
        print(f"响应内容: {e.response.text}")
finally:
    files['file'].close()