# Chat IOC Service

Chat IOC Service 是一个基于控制反转（IoC）容器的聊天应用后端服务。

## 项目特性

- 基于自研 IoC 容器的依赖注入
- 模块化架构设计
- 完整的首页接口服务
- 健康检查和心跳检测
- 面向接口编程
- 用户认证和授权功能
- 数据库持久化支持

## 技术栈

- Java 17
- Maven
- 自研 IOC 容器
- H2 嵌入式数据库
- JUnit 5 (测试)

## 项目结构

```
src/
├── main/
│   └── java/
│       └── com/chat/ioc/
│           ├── Application.java              # 应用启动类
│           ├── HttpServerApplication.java    # HTTP服务器启动类
│           ├── IOCContainer.java             # IoC 容器核心实现
│           ├── Autowired.java                # 依赖注入注解
│           ├── config/
│           │   └── AppConfig.java            # 应用配置
│           ├── controller/
│           │   ├── AuthController.java       # 认证控制器
│           │   └── HomeController.java       # 首页控制器
│           ├── entity/
│           │   ├── ApiResponse.java          # API 响应封装
│           │   ├── ChatMessage.java          # 聊天消息实体
│           │   ├── ChatSession.java          # 聊天会话实体
│           │   ├── HomePageInfo.java         # 首页信息实体
│           │   ├── LoginRequest.java         # 登录请求实体
│           │   ├── LoginResponse.java        # 登录响应实体
│           │   └── User.java                 # 用户实体
│           ├── service/
│           │   ├── AuthService.java          # 认证服务接口
│           │   ├── AuthServiceImpl.java      # 认证服务实现
│           │   ├── HomePageService.java      # 首页服务接口
│           │   └── HomePageServiceImpl.java  # 首页服务实现
│           ├── database/
│           │   └── DatabaseManager.java      # 数据库管理类
│           └── server/
│               └── SimpleHttpServer.java     # 简单HTTP服务器
└── test/
    └── java/
        └── com/chat/ioc/
            └── SimpleIOCContainerTest.java   # 测试类
```

## API 接口

详见 [API 文档](API_DOCUMENTATION.md)

## 快速开始

### 编译项目

```bash
mvn clean compile
```

### 运行项目

```bash
# 运行基本应用
mvn exec:java -Dexec.mainClass="com.chat.ioc.Application"

# 运行HTTP服务器（推荐用于Web API访问）
mvn exec:java -Dexec.mainClass="com.chat.ioc.HttpServerApplication"
# 或者指定端口
java -cp target/chat-ioc-service-1.0.0-SNAPSHOT-shaded.jar com.chat.ioc.HttpServerApplication 8080
```

### 打包项目

```bash
mvn clean package -DskipTests
```

### 运行打包后的应用

```bash
java -cp target/chat-ioc-service-1.0.0-SNAPSHOT-shaded.jar com.chat.ioc.HttpServerApplication
```

## 核心功能

1. **IoC 容器** - 实现了依赖注入和控制反转
2. **首页服务** - 提供首页信息、健康检查、心跳检测等功能
3. **认证服务** - 提供用户登录、登出等认证功能
4. **数据库持久化** - 使用H2数据库存储用户信息
5. **API 响应** - 统一的 API 响应格式
6. **配置管理** - 基于 IoC 容器的应用配置

## 测试

运行所有测试：

```bash
mvn test
```

## 默认用户

- 用户名：admin
- 密码：password

## 许可证

MIT License