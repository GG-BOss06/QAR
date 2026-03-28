import pandas as pd
import json
import requests
import sys

# 重定向输出到文件
log_file = open('encryption_log.txt', 'w', encoding='utf-8')
original_stdout = sys.stdout
sys.stdout = log_file

try:
    # 读取Excel文件
    df = pd.read_excel('QAR示例数据.xlsx')

    print("=== QAR示例数据内容 ===")
    print(df.to_string())
    print(f"\n=== 数据形状 ===")
    print(f"行数: {len(df)}, 列数: {len(df.columns)}")
    print(f"\n=== 列名 ===")
    print(df.columns.tolist())

    # 转换为JSON格式
    json_data = df.to_json(orient='records', force_ascii=False)
    print(f"\n=== JSON格式数据 (前1000字符) ===")
    print(json_data[:1000])

    # 发送到加密端点
    payload = {
        "data": json_data,
        "policy": "role:admin AND department:security"
    }

    try:
        response = requests.post('http://localhost:8081/api/encrypt', json=payload)
        response.raise_for_status()
        
        result = response.json()
        print("\n\n=== 加密结果 ===")
        print(f"Status Code: {result['code']}")
        print(f"Message: {result['message']}")
        print(f"\nEncrypted Data (first 300 chars): {result['encryptedData'][:300]}...")
        print(f"Wrapped Key: {result['wrappedKey']}")
        
        # 保存加密结果到新文件
        with open('encrypted_qar_data.json', 'w', encoding='utf-8') as f:
            json.dump(result, f, ensure_ascii=False, indent=2)
        print("\n加密结果已保存到 encrypted_qar_data.json")
        
    except Exception as e:
        print(f"\nError: {e}")
        if hasattr(e, 'response') and e.response:
            print(f"Response status: {e.response.status_code}")
            print(f"Response content: {e.response.text}")

finally:
    sys.stdout = original_stdout
    log_file.close()
    print("处理完成，日志已保存到 encryption_log.txt")