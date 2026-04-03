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

## 错误码说明
- 200: 操作成功
- 400: 请求参数错误
- 401: 未授权
- 403: 禁止访问
- 404: 资源不存在
- 500: 服务器内部错误

## 技术栈
- Java 17
- 自研 IoC 容器
- Maven 项目管理

## 部署说明
1. 确保系统已安装 Java 17
2. 使用 Maven 编译项目：`mvn clean package`
3. 运行应用：`java -jar target/chat-ioc-service-1.0.0-SNAPSHOT-shaded.jar`