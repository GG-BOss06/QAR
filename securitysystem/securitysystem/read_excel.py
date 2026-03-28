import pandas as pd
import json

# 读取Excel文件
df = pd.read_excel('e:\\stone2026project\\QAR示例数据.xlsx')

# 保存到文本文件
with open('excel_content.txt', 'w', encoding='utf-8') as f:
    f.write("=== QAR示例数据内容 ===\n")
    f.write(df.to_string())
    f.write(f"\n\n=== 数据形状 ===\n")
    f.write(f"行数: {len(df)}, 列数: {len(df.columns)}\n")
    f.write(f"\n=== 列名 ===\n")
    f.write(str(df.columns.tolist()))
    
    # 转换为JSON格式
    json_data = df.to_json(orient='records', force_ascii=False)
    f.write(f"\n\n=== JSON格式数据 ===\n")
    f.write(json_data)

print("Excel内容已保存到 excel_content.txt")