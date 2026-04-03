# Chat IOC Service

这是一个为聊天应用设计的控制反转（IoC）容器和服务管理框架。

## 项目结构

```
chat-ioc-service/
├── pom.xml                    # Maven 配置文件
├── src/
│   ├── main/java/            # 主代码源文件
│   │   └── com/chat/ioc/     # 核心代码包
│   │       ├── IOCContainer.java    # IOC 容器核心实现
│   │       ├── Autowired.java       # 依赖注入注解
│   │       └── example/             # 示例应用
│   │           ├── ChatApplication.java
│   │           ├── ChatService.java
│   │           ├── MessageService.java
│   │           ├── NotificationService.java
│   │           ├── UserService.java
│   │           ├── EmailMessageService.java
│   │           ├── PushNotificationService.java
│   │           ├── DatabaseUserService.java
│   │           └── model/
│   │               └── User.java
│   └── test/java/            # 测试代码源文件
│       └── com/chat/ioc/     # 测试代码包
│           ├── IOCContainerTest.java
│           ├── AdvancedIOCContainerTest.java
│           └── SimpleIOCContainerTest.java
└── README.md                 # 项目说明文档
```

## 技术栈

- Java 17
- Maven 3.9+
- JUnit 5 (测试框架)
- Mockito (模拟对象)
- AssertJ (断言库)

## 开发规范

本项目采用 TDD（测试驱动开发）模式：

1. 首先编写测试用例，定义期望的功能
2. 编写最少量的代码使测试通过
3. 重构代码，保持测试通过
4. 重复上述过程

## 运行测试

```bash
# 编译项目
mvn compile

# 运行所有测试
mvn test

# 运行特定测试
mvn test -Dtest=SimpleIOCContainerTest

# 查看测试报告
mvn surefire-report:report
```

## 当前实现

当前已实现的 IoC 容器功能：

- Bean 注册与获取
- 单例与原型作用域支持
- 依赖注入（字段注入）
- 循环依赖检测
- Bean 生命周期管理（初始化方法）

## 已完成功能

1. **基础 IOC 容器** - 实现了基本的 Bean 管理功能
2. **作用域管理** - 支持单例和原型作用域
3. **依赖注入** - 通过 @Autowired 注解实现字段注入
4. **循环依赖检测** - 防止无限递归实例化
5. **生命周期管理** - 支持初始化方法调用

## 示例应用

项目包含一个聊天应用示例，演示了 IOC 容器的实际应用：

- MessageService, NotificationService, UserService 接口及其实现
- ChatService 使用依赖注入获取所需服务
- 控制台演示程序展示容器工作原理