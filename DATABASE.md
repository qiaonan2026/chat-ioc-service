# 数据库（H2）与 DBeaver 使用手册

本项目使用 **H2 嵌入式文件数据库**，数据库文件默认位于项目根目录：`chat_ioc_db.mv.db`。

> 重要：`*.mv.db` 是二进制文件，**不要**用文本编辑器直接修改。应通过 JDBC 客户端（如 DBeaver）连接后执行 SQL 读写。

---

## 1. 是否需要提交 `chat_ioc_db.mv.db`？

- **不建议提交**：属于本地运行数据/测试数据，容易造成数据冲突、仓库膨胀，并可能包含敏感信息。
- **推荐做法**：将其加入 `.gitignore`，由程序启动自动创建/初始化。

---

## 2. 安装 DBeaver（macOS）

### 方式 A：Homebrew（推荐）

```bash
brew install --cask dbeaver-community
```

### 方式 B：官网下载

下载安装 DBeaver Community（macOS 版本）后打开即可。

---

## 3. 在 DBeaver 创建正确的 H2 连接（关键）

### 3.1 新建连接并选择驱动

菜单：`Database` → `New Database Connection`

- 驱动选择：**H2**
  - 若有子选项：选 **Embedded / File**（文件模式）
- **不要选**：Oracle / MySQL / PostgreSQL 等

### 3.2 正确填写连接信息（推荐绝对路径）

本项目代码中使用的连接串为 `jdbc:h2:./chat_ioc_db`，其中 `./` 依赖“启动工作目录”。为了避免连错库，DBeaver 请使用 **绝对路径**：

- **JDBC URL（推荐）**：

```text
jdbc:h2:file:/Users/zhaodongbo1/Documents/GitHub/chat-ioc-service/chat_ioc_db
```

注意：
- **不要**写成 `.../chat_ioc_db.mv.db`（不要带后缀）
- **要**写 `file:` 前缀

- **User**：`sa`
- **Password**：空

点击 **Test Connection** 成功后保存。

---

## 4. 刚刚问题复盘：`Table "USERS" not found (this database is empty)`

### 现象

执行查询时报错：

```text
SQL Error [42104] [42S04]: Table "USERS" not found (this database is empty)
```

### 根因（最常见）

在 DBeaver 里使用了相对路径 URL（例如 `jdbc:h2:./chat_ioc_db`），`./` 会以 **DBeaver 的工作目录** 为基准，从而连接到一个“新建的空库”，因此没有 `users` 表。

### 解决

改用绝对路径连接（见第 3.2 节）。

---

## 5. 自检：确认已连到正确数据库

### 5.1 查看所有表

```sql
SHOW TABLES;
```

### 5.2 通过 INFORMATION_SCHEMA 查看表（更通用）

```sql
SELECT TABLE_SCHEMA, TABLE_NAME
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_TYPE = 'BASE TABLE'
ORDER BY TABLE_SCHEMA, TABLE_NAME;
```

如果连对了库，应该能看到 `USERS` 表（或至少不是空）。

---

## 6. 表结构（`users`）

`users` 表由后端启动时自动创建（`DatabaseManager.initializeDatabase()`），核心字段：

- `id`：自增主键
- `username`：唯一用户名
- `password`：密码（当前为明文示例，生产应加密）
- `email`、`nickname`
- `is_active`：是否启用
- `created_at`、`updated_at`

---

## 7. 常用查询 SQL（可直接在 DBeaver 执行）

> 说明：`?` 是预编译占位符。DBeaver 里直接执行时，请改成具体值（例如 `'admin'`）。

### 7.1 按用户名查询

```sql
SELECT id, username, password, email, nickname, is_active, created_at, updated_at
FROM users
WHERE username = 'admin' AND is_active = TRUE;
```

### 7.2 按 ID 查询（/api/me 常用）

```sql
SELECT id, username, password, email, nickname, is_active, created_at, updated_at
FROM users
WHERE id = 1 AND is_active = TRUE;
```

### 7.3 查询全部启用用户

```sql
SELECT id, username, email, nickname, is_active, created_at, updated_at
FROM users
WHERE is_active = TRUE
ORDER BY id DESC;
```

---

## 8. 导入/导出建议

- **不建议**通过提交 `.mv.db` 文件共享数据
- 推荐：
  - 导出/维护 SQL 脚本（建表、初始化数据）
  - 在 DBeaver 中通过 `Export Data` / `Import Data` 或执行 SQL 脚本完成数据迁移

---

## 9. 为什么会“突然新增很多文件”？（构建产物 vs 运行数据）

### 9.1 `target/` 目录（Maven 构建产物，**不应提交**）

当你执行以下命令之一时：

- `mvn clean compile`
- `mvn test`
- `mvn package -DskipTests`
- `mvn exec:java ...`

Maven 会在项目根目录自动生成/更新 `target/`，其中常见内容包括：

- `target/classes/**`：编译后的 `.class`
- `target/test-classes/**`：测试编译产物
- `target/surefire-reports/**`：单元测试报告（本次你看到的新增 XML/TXT 就在这里）
- `target/chat-ioc-service-*.jar` / `target/original-*.jar`：打包生成的 jar
- `target/maven-status/**`、`target/maven-archiver/**`：Maven 元数据

这些文件数量多、每次构建都会变动，属于**可再生文件**，因此应当通过 `.gitignore` 忽略（本仓库已加入 `target/` 忽略规则）。

### 9.2 `chat_ioc_db*.mv.db`（H2 本地运行数据，**不建议提交**）

这是 H2 嵌入式文件数据库在本地运行时生成/更新的二进制文件，内容依赖你的本地运行与测试数据。

本仓库已在 `.gitignore` 中忽略：

- `chat_ioc_db*.mv.db`
- `chat_ioc_db*.trace.db`

### 9.3 如何快速清理这些新增文件

- 仅清理构建产物：

```bash
mvn clean
```

- 彻底清理（包括测试报告等）：

```bash
rm -rf target/
```

---

## 10. 常见坑：DBeaver 占用导致服务登录 500

### 现象

登录接口返回：

```json
{"code":500,"message":"Internal server error during login: Failed to find user by username","data":null}
```

同时服务端日志可能出现类似：

```text
Database may be already in use ... The file is locked: .../chat_ioc_db.mv.db
```

### 根因

H2 以“嵌入式文件模式”打开数据库时，会对 `chat_ioc_db.mv.db` 加文件锁；如果 DBeaver 正在打开该文件（或以嵌入式方式连接），服务进程将无法获得锁，初始化数据库/查询时就会失败，表现为登录 500。

### 解决

- 关闭 DBeaver 里对应连接（或退出 DBeaver），释放文件锁
- 再重启服务

> 建议：不要让 DBeaver 与服务同时以“嵌入式文件模式”打开同一个 db 文件；如需并发访问，应改为 H2 server mode（本项目当前默认是嵌入式模式）。
