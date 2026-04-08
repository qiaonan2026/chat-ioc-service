# Chat IOC Service - API 接口文档

## 项目概述
Chat IOC Service 是一个基于控制反转（IoC）容器的聊天应用后端服务，提供灵活的依赖注入和可扩展的服务架构。

## 首页接口

### 1. 获取首页信息
- **接口地址**：`GET /api/home`
- **功能描述**：获取服务首页信息，包括服务标题、描述、版本等基本信息
- **请求参数**：无
- **返回示例**：
```json
{
  "code": 200,
  "message": "Welcome to Chat IOC Service",
  "data": {
    "title": "Chat IOC Service",
    "description": "A robust chat application backend service with IoC container",
    "version": "1.0.0",
    "serverTime": "2026-04-03T18:45:00",
    "environment": "development",
    "activeUsers": 0,
    "status": "UP"
  }
}
```
- **错误码**：
  - 200: 成功
  - 500: 服务器内部错误

### 2. 获取详细首页信息
- **接口地址**：`GET /api/home/detail`
- **功能描述**：获取详细的首页信息，包含更全面的服务状态信息
- **请求参数**：无
- **返回示例**：
```json
{
  "code": 200,
  "message": "Detailed home page info retrieved successfully",
  "data": {
    "title": "Chat IOC Service",
    "description": "A robust chat application backend service with IoC container",
    "version": "1.0.0",
    "serverTime": "2026-04-03T18:45:00",
    "environment": "development",
    "activeUsers": 0,
    "status": "UP"
  }
}
```
- **错误码**：
  - 200: 成功
  - 500: 服务器内部错误

### 3. 心跳检测
- **接口地址**：`GET /api/ping`
- **功能描述**：心跳检测接口，用于检测服务可用性
- **请求参数**：无
- **返回示例**：
```json
{
  "code": 200,
  "message": "Ping successful",
  "data": "pong"
}
```
- **错误码**：
  - 200: 成功
  - 500: 服务器内部错误

### 4. 健康检查
- **接口地址**：`GET /api/health`
- **功能描述**：健康检查接口，返回服务的整体健康状态
- **请求参数**：无
- **返回示例**：
```json
{
  "code": 200,
  "message": "Health check passed",
  "data": {
    "title": "Chat IOC Service",
    "description": "A robust chat application backend service with IoC container",
    "version": "1.0.0",
    "serverTime": "2026-04-03T18:45:00",
    "environment": "development",
    "activeUsers": 0,
    "status": "UP"
  }
}
```
- **错误码**：
  - 200: 成功
  - 500: 服务器内部错误

## 认证接口

### 1. 用户登录
- **接口地址**：`POST /api/login`
- **功能描述**：用户登录认证接口，验证用户名和密码并返回认证令牌
- **请求参数**：
```json
{
  "username": "用户名",
  "password": "密码"
}
```
- **返回示例（成功）**：
```json
{
  "code": 200,
  "message": "Login successful",
  "data": {
    "success": true,
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user": {
      "id": 1,
      "username": "admin",
      "email": "admin@example.com",
      "nickname": "Administrator"
    },
    "message": "Login successful"
  }
}
```
- **返回示例（失败）**：
```json
{
  "code": 401,
  "message": "Invalid credentials",
  "data": {
    "success": false,
    "token": null,
    "user": null,
    "message": "Invalid credentials"
  }
}
```
- **错误码**：
  - 200: 登录成功
  - 400: 请求参数错误
  - 401: 用户名或密码错误
  - 500: 服务器内部错误

### 2. 用户登出
- **接口地址**：`POST /api/logout`
- **功能描述**：用户登出接口，使认证令牌失效
- **请求参数**：
```json
{
  "token": "认证令牌"
}
```
- **返回示例**：
```json
{
  "code": 200,
  "message": "Logged out successfully",
  "data": "Logged out successfully"
}
```
- **错误码**：
  - 200: 登出成功
  - 400: 请求参数错误
  - 500: 服务器内部错误

## 聊天会话接口

> 说明：聊天接口支持三种响应方式（JSON / SSE / NDJSON），用于兼容不同前端实现。
> - **JSON**：常规请求（xhr/axios）一次性返回
> - **SSE**：`text/event-stream`（浏览器标准流式）
> - **NDJSON**：`application/x-ndjson`（逐行 JSON 流，fetch 读流更方便）
>
> **重要约束**：`POST /api/chat` 必须提供有效的 `sessionId`（不能为空/不能为 null），否则返回 400。

### 1. 创建会话
- **接口地址**：`POST /api/chat/session`
- **功能描述**：创建一个新的聊天会话，返回 `sessionId`
- **请求参数**：无
- **返回示例**：
```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "sessionId": "d4cb120cc3564d63874a5cbea6e2c5ec",
    "createdAt": "2026-04-08T11:15:16.362324Z"
  }
}
```

### 2. 查询会话列表（按最近活跃排序）
- **接口地址**：`GET /api/chat/sessions`
- **功能描述**：分页返回会话列表，按 `updatedAt` 倒序（最近活跃的会话在最前）
- **请求参数**：
  - `limit`：可选，默认 20，最大 200
  - `offset`：可选，默认 0
- **返回示例**：
```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "limit": 5,
    "offset": 0,
    "sessions": [
      {
        "sessionId": "0f8cf5af36fb490aa13336b2fefbcf36",
        "createdAt": "2026-04-08T11:57:48.195495Z",
        "updatedAt": "2026-04-08T11:57:54.521343Z"
      }
    ]
  }
}
```

### 3. 查询会话历史消息
- **接口地址**：`GET /api/chat/session?sessionId=<sessionId>`
- **功能描述**：查询指定会话下的历史消息（按 id 升序）
- **请求参数**：
  - `sessionId`：必填
- **返回示例**：
```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "sessionId": "d4cb120cc3564d63874a5cbea6e2c5ec",
    "messages": [
      { "id": 1, "role": "user", "content": "你好", "createdAt": "2026-04-08T11:15:20.000Z" },
      { "id": 2, "role": "assistant", "content": "你说： 你好", "createdAt": "2026-04-08T11:15:21.000Z" }
    ]
  }
}
```

### 4. 删除会话（含消息）
- **接口地址**：`DELETE /api/chat/session?sessionId=<sessionId>`
- **功能描述**：删除会话以及该会话下的所有消息
- **请求参数**：
  - `sessionId`：必填
- **返回示例**：
```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "deleted": true,
    "sessionId": "d4cb120cc3564d63874a5cbea6e2c5ec"
  }
}
```

### 5. 发送消息（支持流式）
- **接口地址**：`POST /api/chat`
- **功能描述**：向指定会话发送消息。支持以下入参字段：
  - `content`：推荐字段
  - `message`：兼容字段
- **请求参数**：
```json
{
  "content": "啊啊啊",
  "sessionId": "d4cb120cc3564d63874a5cbea6e2c5ec"
}
```

#### 5.1 JSON（默认，兼容 xhr/axios）
- **请求头**：`Accept: application/json`
- **返回示例**：
```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "sessionId": "d4cb120cc3564d63874a5cbea6e2c5ec",
    "reply": "你说： 啊啊啊"
  }
}
```

#### 5.2 SSE（`text/event-stream`）
- **请求头**：`Accept: text/event-stream`
- **返回格式**：服务端会发送 `start -> delta* -> done` 事件序列。每个事件的 `data:` 是一行 JSON。
- **示例片段**：
```text
event: message
data: {"code":200,"message":"OK","data":{"event":"start","sessionId":"d4cb120cc3564d63874a5cbea6e2c5ec"}}

event: message
data: {"code":200,"message":"OK","data":{"event":"delta","delta":"你"}}

event: message
data: {"code":200,"message":"OK","data":{"event":"done"}}
```

#### 5.3 NDJSON（逐行 JSON 流）
- **触发方式**：`POST /api/chat?stream=1`（或请求体带 `"stream": true`，或请求头 `X-Stream: 1`）
- **返回头**：`Content-Type: application/x-ndjson`
- **返回格式**：每行一个 JSON，对应 `start -> delta* -> done`
- **示例片段**：
```text
{"code":200,"message":"OK","data":{"event":"start","sessionId":"d4cb120cc3564d63874a5cbea6e2c5ec"}}
{"code":200,"message":"OK","data":{"event":"delta","delta":"你"}}
{"code":200,"message":"OK","data":{"event":"done"}}
```

#### 5.4 前端如何根据字段取值（通用解析规则）
- **事件字段**：统一读取 `data.event`
  - `start`：开始，读取 `data.sessionId`
  - `delta`：增量内容，读取 `data.delta` 并追加到当前 assistant 文本
  - `done`：结束
  - `error`：错误（或 `code != 200`）

### 聊天接口错误码
- 200：成功（JSON 或流式事件）
- 400：缺少/非法参数（例如 `sessionId` 为空）
- 404：会话不存在
- 500：服务器内部错误

## 错误码说明
- 200: 操作成功
- 400: 请求参数错误
- 401: 未授权（用户名或密码错误）
- 403: 禁止访问
- 404: 资源不存在
- 500: 服务器内部错误

## 技术栈
- Java 17
- 自研 IoC 容器
- H2 嵌入式数据库
- Maven 项目管理

## 部署说明
1. 确保系统已安装 Java 17
2. 使用 Maven 编译项目：`mvn clean package -DskipTests`
3. 运行 HTTP 服务器：`java -cp target/chat-ioc-service-1.0.0-SNAPSHOT-shaded.jar com.chat.ioc.HttpServerApplication`
4. 默认端口为 8080，可通过命令行参数指定其他端口：`java -cp target/chat-ioc-service-1.0.0-SNAPSHOT-shaded.jar com.chat.ioc.HttpServerApplication 8081`

## 数据库说明
- 使用 H2 嵌入式数据库
- 数据文件存储在本地 `chat_ioc_db` 文件中
- 默认管理员用户：username=admin, password=password