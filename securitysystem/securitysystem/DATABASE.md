# PostgreSQL数据库配置

项目默认连接PostgreSQL，不再使用H2作为运行时数据库。

## 1. 创建数据库

先在PostgreSQL中创建数据库和账号，示例：

```sql
CREATE DATABASE qar;
CREATE USER qar_app WITH PASSWORD 'qar_app';
GRANT ALL PRIVILEGES ON DATABASE qar TO qar_app;
```

如果你直接使用本机`postgres`账号，也可以只创建数据库：

```sql
CREATE DATABASE qar;
```

## 2. PowerShell启动参数

在Windows PowerShell里设置环境变量后启动：

```powershell
$env:APP_DB_URL="jdbc:postgresql://127.0.0.1:5432/qar"
$env:APP_DB_USERNAME="postgres"
$env:APP_DB_PASSWORD="postgres"
.\mvnw.cmd spring-boot:run
```

如果你使用单独应用账号：

```powershell
$env:APP_DB_URL="jdbc:postgresql://127.0.0.1:5432/qar"
$env:APP_DB_USERNAME="qar_app"
$env:APP_DB_PASSWORD="qar_app"
.\mvnw.cmd spring-boot:run
```

## 3. 当前默认行为

- `application.properties`默认连`jdbc:postgresql://127.0.0.1:5432/qar`
- `ddl-auto=update`，首次启动会自动建表
- 测试环境仍使用独立测试配置，不影响正式库

## 4. 常见检查项

- PostgreSQL服务已启动
- `qar`数据库已存在
- 账号有建表权限
- 8101端口没有被旧服务占用
