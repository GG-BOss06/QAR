# QAR 安全系统 (QAR Security System)

QAR 安全系统是一个面向航空领域 QAR（快速访问记录器）数据的综合性安全管理平台。系统采用**客户端加密架构**，核心目标是实现明文数据永不上传服务器、高强度数据加密存储、细粒度的基于角色的访问控制（RBAC）以及完整可追溯的审计日志。

***

## 项目架构

项目由两个核心服务组成，采用**客户端加密 + 服务端存储密文**的混合架构：

### 1. `securitysystem` (主业务系统)

基于 **Spring Boot 4.x** 构建的 Web 服务，负责核心业务逻辑。

- **端口**: `8101`
- **主要职责**:
  - 用户认证与授权（无状态 Session + HttpOnly Cookie）
  - 管理员后台（人员管理、注册审批、全量文件导出）
  - 密文文件存储与管理（仅存储加密后的数据）
  - 系统反馈收集与处理
  - 全局 API 审计日志记录

### 2. `local-crypto-service` (本地加密服务)

一个轻量级的独立 Java 服务，在用户本地运行，专职处理高强度的加密运算。

- **端口**: `18234`
- **主要职责**:
  - 提供 `/encrypt` 和 `/decrypt` 等 RESTful 接口
  - 采用 **BouncyCastle** 引擎实现 `AES-256-GCM` 认证加密
  - 密钥生成、IV 向量管理以及模拟 L-ABE（属性基加密）的密钥封装
  - **确保明文数据永远不会传输到服务器端**

***

## 核心特性

### 安全架构

- **客户端加密**: 明文数据在本地加密服务中处理，服务器仅存储密文
- **AES-256-GCM 加密**: 采用 BouncyCastle 提供的 AES-256-GCM 算法，确保数据机密性与防篡改
- **L-ABE 密钥封装**: 模拟属性基加密的密钥管理机制，为后续量子抗性做准备
- **三权分立**: 用户密码、L-ABE 密钥、服务器密文三方制约，缺一不可

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
- **密文文件**: 上传的 QAR 密文以 `.enc` 物理文件形式存储在 `securitysystem/securitysystem/data/uploads/` 目录

***

## 快速开始

### 环境要求

- JDK 21
- Maven 3.9+
- 现代浏览器（支持 ES6+）

### 1. 启动本地加密服务

进入 `local-crypto-service` 目录，编译并启动服务：

```bash
cd local-crypto-service
.\mvnw clean package -DskipTests
.\cryptionstart.bat  # Windows 环境下运行
```

服务将在 `http://127.0.0.1:18234` 启动，可通过 `/health` 接口验证：

```bash
curl http://127.0.0.1:18234/health
```

### 2. 启动主业务系统

进入 `securitysystem/securitysystem` 目录，启动 Spring Boot 应用：

```bash
cd securitysystem/securitysystem
.\mvnw spring-boot:run
```

主系统将在 `http://localhost:8101` 启动。

### 3. 访问系统

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
3. 前端自动调用本地加密服务进行加密
4. 加密完成后，密文上传至服务器
5. 服务器仅存储密文，无法获取明文

### 文件下载流程

1. 在工作台选择要下载的文件
2. 服务器返回密文数据
3. 前端调用本地加密服务进行解密
4. 解密后用户获得原始明文文件

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
| `/api/files/upload` | POST | 上传加密文件 |
| `/api/files/download/{id}` | GET | 下载加密文件 |
| `/api/files` | GET | 获取文件列表 |

### 管理员接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/admin/users` | GET | 获取用户列表 |
| `/api/admin/persons` | GET | 获取人员档案列表 |
| `/api/admin/files` | GET | 获取所有文件列表 |
| `/api/admin/account-requests` | GET | 获取待审批账号申请 |
| `/api/admin/account-requests/{id}/approve` | POST | 批准账号申请 |
| `/api/admin/account-requests/{id}/reject` | POST | 驳回账号申请 |

### 本地加密服务接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/encrypt` | POST | 加密数据 |
| `/decrypt` | POST | 解密数据 |
| `/keys` | GET | 获取密钥状态 |
| `/health` | GET | 健康检查 |

***

## 项目结构

```
QAR/
├── local-crypto-service/          # 本地加密服务
│   ├── src/main/java/com/qar/crypto/
│   │   ├── AesCryptoService.java  # AES加密核心实现
│   │   ├── KeyStore.java          # 密钥存储管理
│   │   └── LocalCryptoService.java # HTTP服务入口
│   └── cryptionstart.bat          # Windows启动脚本
│
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
│       └── uploads/               # 加密文件存储
│
├── README.md                      # 项目说明文档
└── wireshark_capture_guide.md     # Wireshark抓包指南
```

***

## 技术栈

- **后端**: Java 21, Spring Boot 4.x, Spring Security, Spring Data JPA, H2 Database
- **前端**: HTML5, Vanilla JavaScript, CSS3
- **加密**: BouncyCastle (bcprov-jdk18on), AES-256-GCM
- **构建**: Maven Wrapper

***

## 安全设计

### 数据流向

```
[用户明文文件] 
    ↓ (本地加密服务 AES-256-GCM)
[密文数据] 
    ↓ (HTTPS传输)
[服务器存储密文]
    ↓ (用户请求下载)
[密文数据返回]
    ↓ (本地解密)
[用户获得明文文件]
```

### 密钥管理

- **AES密钥**: 由本地加密服务生成，存储在用户本地
- **L-ABE封装**: AES密钥经过模拟L-ABE封装后传输
- **服务器**: 仅存储密文，无法获取明文或密钥

***

## 最新进展与修复

### v1.0.0 (当前版本)

- **[Security] 修复管理员审批 403 错误**:
  - 修复了 Spring Security 6 中默认开启 XOR CSRF 令牌保护导致前端（SPA）请求被拦截的问题
  - 配置 `CsrfTokenRequestAttributeHandler` 禁用了 XOR 保护并解决了延迟加载问题，确保管理员能够正常通过/驳回注册申请
  - 统一将安全配置中的 `.hasRole("ADMIN")` 调整为 `.hasAuthority("ROLE_ADMIN")` 以精准匹配权限

- **[Crypto] BouncyCastle 引擎集成**:
  - 将加解密核心从 JDK 默认 JCE 切换为 BouncyCastle (`bcprov-jdk18on`)
  - 修复了因为打包成 Fat JAR 导致 BC 签名验证失败（`JCE cannot authenticate the provider BC`）的问题，改用 `maven-dependency-plugin` 独立加载依赖
  - 废弃硬编码的测试密钥，实现了真正的 AES-256 随机密钥生成与模拟 L-ABE 的动态封装

- **[Encoding] UTF-8 全链路编码**:
  - 解决了中文数据在存储、传输、显示过程中的乱码问题
  - 添加了全局字符编码过滤器，确保所有HTTP请求和响应都使用UTF-8编码
  - 配置了服务器端编码设置，保证JSON响应正确显示中文

- **[Feature] 客户端加密架构**:
  - 实现了完整的客户端加密流程，明文数据永不上传服务器
  - 前端自动调用本地加密服务进行加解密操作
  - 服务器仅存储密文，无法获取明文内容

***

## 开发与测试

### Wireshark 抓包验证

可以使用 Wireshark 捕获和分析系统的加密通信，验证安全性。详细操作指南请参考：

👉 [Wireshark 抓包指南](./wireshark_capture_guide.md)

**验证要点：**
- ✓ 主服务器（8101）从未收到明文文件内容
- ✓ 本地加密服务（18234）处理所有加解密操作
- ✓ 密文数据正确传输和存储

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
