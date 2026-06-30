# QAR 安全系统 (QAR Security System)

QAR 安全系统是一个面向航空领域 QAR（快速访问记录器）数据的综合性安全管理平台。系统当前采用**应用层加密传输 + 文件密文存储**的原型架构，核心目标是逐步实现高强度数据加密存储、细粒度访问控制以及面向 `L-ABE 封装 AES 密钥` 的后量子可控共享能力。

***

## 项目架构

项目基于 **Spring Boot 4.x** 构建的 Web 服务，采用前后端分离架构：

### 主业务系统 (securitysystem)

- **端口**: `8101`
- **主要职责**:
  - 用户认证与授权（无状态 Session + HttpOnly Cookie）
  - 管理员后台（人员管理、注册审批、全量文件导出）
  - 文件存储与管理
  - 系统反馈收集与处理
  - 全局 API 审计日志记录
  - 应用层加密传输与文件密文存储

***

## 核心特性

### 安全架构

- **应用层加密传输**: 浏览器使用 Web Crypto API 按请求生成临时 AES 密钥，并使用服务端公钥进行封装传输
- **AES-256-GCM 加密**: 服务端统一优先使用 BouncyCastle 实现 AES-256-GCM，用于文件密文存储与数据完整性保护
- **密钥独立封装**: 文件 AES 密钥与文件主体分离处理，当前以 RSA-OAEP 作为中期过渡封装方案，最终目标为 L-ABE 封装 AES 密钥

### 用户管理

- **严格的准入审批流**: 新用户注册采用"申请-审批"机制，必须由管理员审核通过后方可正式创建账号
- **档案库匹配**: 注册信息必须与系统预设的人员档案库匹配（工号、姓名、身份证后四位、联系方式等）
- **动态数据种子**: 系统启动时自动加载 `person_seed.csv` 初始化人员档案

### 安全防护

- **双重 CSRF 防护**: 在 Stateless 架构下结合 CookieCsrfTokenRepository，支持单页应用(SPA)的安全调用
- **UTF-8 全链路编码**: 解决中文数据在存储、传输、显示过程中的编码问题
- **审计日志**: 完整记录用户操作，支持追溯

### 数据持久化

- **数据库**: H2 内存数据库，数据存储在内存中
- **文件记录**: 文件元数据和密文字段存储在数据库中
- **文件内容**: 当前上传文件会先加密后再保存，不再按明文直接存储

***

## 快速开始

### 环境要求

- JDK 21
- Maven 3.9+
- 现代浏览器（支持 ES6+）

### 启动主业务系统

进入 `securitysystem/securitysystem` 目录，启动 Spring Boot 应用：

```bash
cd securitysystem/securitysystem
.\mvnw spring-boot:run
```

主系统将在 `http://localhost:8101` 启动。

### 访问系统

- **入口**: 打开浏览器访问 http://localhost:8101/auth.html
- **初始管理员账号**: `admin`
- **初始管理员密码**: `CAUCqar`

***

## 用户指南

### 用户注册流程

1. 访问登录页面，点击"注册账号"
2. 填写个人信息（必须与档案库匹配）：
   - 工号（作为登录账号）
   - 姓名
   - 身份证后四位
   - 联系方式
   - 航司
   - 职位
   - 部门
3. 设置密码并确认
4. 提交申请，等待管理员审批
5. 审批通过后即可使用工号登录

### 文件上传流程

1. 登录系统后进入工作台
2. 选择要上传的文件
3. 前端自动进行传输加密
4. 加密完成后上传至服务器

### 文件下载流程

1. 在工作台选择要下载的文件
2. 服务器返回加密数据
3. 前端自动解密后用户获得原始文件

***

## API 接口

### 认证接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/auth/register` | POST | 用户注册申请 |
| `/api/auth/login` | POST | 用户登录 |
| `/api/auth/logout` | POST | 用户登出 |
| `/api/auth/me` | GET | 获取当前用户信息 |

### 文件接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/files/upload` | POST | 上传文件 |
| `/api/files/download/{id}` | GET | 下载文件 |
| `/api/files` | GET | 获取文件列表 |
| `/api/files/stats` | GET | 获取文件统计 |

### 管理员接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/admin/users` | GET | 获取用户列表 |
| `/api/admin/persons` | GET | 获取人员档案列表 |
| `/api/admin/files` | GET | 获取所有文件列表 |
| `/api/admin/account-requests` | GET | 获取待审批账号申请 |
| `/api/admin/account-requests/{id}/approve` | POST | 批准账号申请 |
| `/api/admin/account-requests/{id}/reject` | POST | 驳回账号申请 |

***

## 项目结构

```
QAR/
├── securitysystem/securitysystem/ # 主业务系统
│   ├── src/main/java/com/qar/securitysystem/
│   │   ├── config/                # 配置类
│   │   ├── controller/            # 控制器
│   │   ├── dto/                   # 数据传输对象
│   │   ├── model/                 # 实体模型
│   │   ├── repo/                  # 数据访问层
│   │   ├── security/              # 安全组件
│   │   ├── service/               # 业务服务
│   │   └── startup/               # 启动初始化
│   ├── src/main/resources/
│   │   ├── static/                # 前端静态文件
│   │   ├── application.properties # 应用配置
│   │   └── person_seed.csv        # 人员档案种子数据
│   └── data/                      # 数据存储目录
│       └── uploads/               # 文件存储
│
├── README.md                      # 项目说明文档
└── wireshark_capture_guide.md     # Wireshark抓包指南
```

***

## 技术栈

- **后端**: Java 21, Spring Boot 4.x, Spring Security, Spring Data JPA, H2 Database
- **前端**: HTML5, Vanilla JavaScript, CSS3, Web Crypto API
- **加密**: AES-256-GCM, RSA-OAEP
- **构建**: Maven Wrapper

***

## 安全设计

### 文件加密流程

```
[用户文件] 
    ↓ (生成 AES-256-GCM 数据密钥)
[文件主体加密]
    ↓ (RSA-OAEP 过渡封装 AES 密钥)
[服务器保存：文件密文 + 封装密钥]
    ↓ (用户请求下载/预览)
[服务端按需解封装并处理]
    ↓ (按场景返回明文或再次加密后的数据)
[用户获得原始文件或可视化结果]
```

### 密钥管理

- **传输临时密钥**: 由浏览器 Web Crypto API 按请求动态生成，不采用固定会话级 AES 明文驻留
- **服务端 RSA 密钥对**: 用于过渡阶段的密钥封装与解封装，由服务端加载或生成并持久化
- **文件数据密钥**: 用于 AES-256-GCM 文件加密，当前由 RSA 过渡封装保存，后续目标替换为 L-ABE 封装
- **服务端能力**: 为支持飞行数据板块预览和管理流程，服务端在授权场景下可以按需解密并解析明文数据

***

## 最新进展与修复

### v1.0.0 (当前版本)

- **[Security] 修复管理员审批 403 错误**:
  - 修复了 Spring Security 6 中默认开启 XOR CSRF 令牌保护导致前端（SPA）请求被拦截的问题
  - 配置 `CsrfTokenRequestAttributeHandler` 禁用了 XOR 保护并解决了延迟加载问题，确保管理员能够正常通过/驳回注册申请
  - 统一将安全配置中的 `.hasRole("ADMIN")` 调整为 `.hasAuthority("ROLE_ADMIN")` 以精准匹配权限

- **[Encoding] UTF-8 全链路编码**:
  - 解决了中文数据在存储、传输、显示过程中的乱码问题
  - 添加了全局字符编码过滤器，确保所有HTTP请求和响应都使用UTF-8编码
  - 配置了服务器端编码设置，保证JSON响应正确显示中文

- **[Feature] 传输层加密**:
  - 实现了应用层加密传输流程
  - 浏览器按请求动态生成 AES-256-GCM 临时密钥
  - 支持以 RSA-OAEP 作为过渡性密钥封装机制

- **[Feature] 文件密文存储**:
  - 上传文件在服务端先进行 AES-256-GCM 加密后再保存
  - AES 数据密钥独立封装，避免与文件主体明文混存
  - 飞行数据板块已支持服务端按需解密后进行明文预览

- **[Feature] 文件统计功能**:
  - 添加了文件统计接口 `/api/files/stats`
  - 显示已上传文件和可用数据数量

***

## 开发与测试

### Wireshark 抓包验证

可以使用 Wireshark 捕获和分析系统的加密通信，验证安全性。详细操作指南请参考：

👉 [Wireshark 抓包指南](./wireshark_capture_guide.md)

**验证要点：**
- ✓ 传输层加密正常工作
- ✓ 数据在传输过程中按请求加密
- ✓ 临时密钥封装传输链路正常

### 测试账号

系统预设了以下测试人员档案（`person_seed.csv`）：

| 工号 | 姓名 | 身份证后四位 | 联系方式 | 航司 | 职位 | 部门 |
|------|------|--------------|----------|------|------|------|
| 20260001 | 张三 | 1234 | 13800000000 | CAUC | 机长 | 飞行一部 |
| 20260002 | 李四 | 5678 | 13900000000 | CAUC | 副驾驶 | 飞行二部 |
| 20260003 | 王五 | 0000 | - | CAUC | 签派员 | 运行控制部 |

***

## 许可证

本项目仅供学习和研究使用。
