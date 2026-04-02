# Spring Boot 101 - Spring 生态学习项目

这是一个全面的 Spring 生态学习项目，旨在帮助开发者深入理解 Spring Framework 和 Spring Boot 框架。

## 📋 项目结构

```
SpringBoot101/
├── src/
│   ├── main/
│   │   ├── java/com/example/springboot/
│   │   │   ├── Application.java          # 主启动类
│   │   │   ├── controller/               # 控制层
│   │   │   ├── service/                  # 业务逻辑层
│   │   │   ├── repository/               # 数据访问层
│   │   │   └── model/                    # 数据模型
│   │   └── resources/
│   │       └── application.properties    # 应用配置
│   └── test/
│       └── java/com/example/springboot/  # 测试代码
├── pom.xml                               # Maven 配置文件
└── README.md                             # 项目说明
```

## 🚀 快速开始

### 环境要求
- Java 17+
- Maven 3.6+

### 安装和运行

1. **克隆或进入项目目录**
   ```bash
   cd SpringBoot101
   ```

2. **编译项目**
   ```bash
   mvn clean compile
   ```

3. **运行项目**
   ```bash
   mvn spring-boot:run
   ```

4. **打包项目**
   ```bash
   mvn clean package
   ```

### 测试 API

项目启动后，可以使用以下 URL 测试：

- **基础问候**: http://localhost:8080/api/hello
- **带参数问候**: http://localhost:8080/api/hello/YourName
- **H2 数据库控制台**: http://localhost:8080/h2-console

## 📚 学习路径

### 第一阶段：基础概念
- [ ] Spring 的核心概念（IoC、DI）
- [ ] Spring Boot 自动配置原理
- [ ] 依赖注入和注解使用

### 第二阶段：Web 开发
- [ ] RESTful API 设计
- [ ] 控制器（Controller）和路由
- [ ] 请求和响应处理
- [ ] 异常处理和错误响应

### 第三阶段：数据持久化
- [ ] Spring Data JPA 基础
- [ ] 数据库操作（CRUD）
- [ ] 关系映射（One-to-One、One-to-Many 等）
- [ ] 事务管理

### 第四阶段：高级特性
- [ ] AOP（面向切面编程）
- [ ] 拦截器和过滤器
- [ ] 缓存机制
- [ ] 日志管理

### 第五阶段：生产级应用
- [ ] 性能优化
- [ ] 安全认证（Spring Security）
- [ ] 项目部署
- [ ] 监控和性能指标

## 🛠️ 核心依赖

| 依赖 | 版本 | 说明 |
|-----|------|------|
| Spring Boot | 3.2.0 | 核心框架 |
| Spring Data JPA | 3.2.0 | 数据访问 |
| Lombok | 1.18.30 | 代码生成工具 |
| H2 Database | 2.2.x | 嵌入式数据库 |

## 📝 学习资源

- [Spring 官方文档](https://spring.io/projects/spring-boot)
- [Spring Data JPA 文档](https://spring.io/projects/spring-data-jpa)
- [RESTful API 设计指南](https://restfulapi.net/)

## 💡 项目特点

- ✅ 清晰的项目结构和分层设计
- ✅ 包含实际示例代码
- ✅ 完整的配置文件说明
- ✅ 逐步的学习路径
- ✅ 开箱即用，无需额外配置

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request 来改进这个学习项目！

## 📄 许可证

MIT License

---

**开始学习 Spring Boot，让我们一起探索 Spring 生态！** 🎉