# Spring Boot 101 - YAML 配置文件总结

## 📋 概述

本项目已从 Properties 格式完全转换为 YAML 格式配置，提供 5 个环境特定的配置文件，满足不同场景的需求。

---

## 📂 配置文件列表

### 1. application.yml（默认/通用配置）
- **用途**: 开发环境、演示环境
- **数据库**: MySQL（主）+ MongoDB（可选）
- **特点**: 平衡的开发配置，日志详细（DEBUG）
- **启动方式**: `mvn spring-boot:run`（无需指定 profile）
- **文件大小**: 121 行
- **适合人群**: 初学者、本地开发

### 2. application-h2.yml（H2 测试配置）
- **用途**: 快速测试、无依赖环境
- **数据库**: H2 内存数据库
- **特点**: 开箱即用，无需安装任何数据库，每次启动自动建表
- **启动方式**: `mvn spring-boot:run -Dspring.profiles.active=h2`
- **文件大小**: 148 行
- **优势**: 零依赖、快速启动、数据隔离
- **Web 控制台**: http://localhost:8080/h2-console
- **适合人群**: 快速体验、初学者、单元测试

### 3. application-mysql.yml（MySQL 生产配置）
- **用途**: 生产环境部署
- **数据库**: MySQL 仅
- **特点**: 性能优化、日志最少（WARN）、禁用非必需功能
- **启动方式**: `mvn spring-boot:run -Dspring.profiles.active=mysql`
- **文件大小**: 120 行
- **连接池**: 最大 30 连接（可根据需要调整）
- **注意事项**: 需要提前创建数据库和表
- **适合人群**: 生产部署、性能测试

### 4. application-mongodb.yml（MongoDB 原始配置）
- **用途**: 迁移前的原始环境
- **数据库**: MongoDB 仅
- **特点**: MongoDB 优化配置，禁用 JPA
- **启动方式**: `mvn spring-boot:run -Dspring.profiles.active=mongodb`
- **文件大小**: 112 行
- **自动索引创建**: true
- **适合人群**: 原始环境运维、迁移前验证

### 5. application-migration.yml（迁移环境配置）
- **用途**: MongoDB→MySQL 数据迁移
- **数据库**: MySQL + MongoDB（双数据源）
- **特点**: 性能优化、详细日志、大 batch size
- **启动方式**: `mvn spring-boot:run -Dspring.profiles.active=migration`
- **文件大小**: 196 行
- **连接池**: MySQL 最大 40 连接
- **Batch Size**: 100（迁移优化）
- **日志**: 单独的迁移日志文件
- **适合人群**: 数据迁移操作、灰度验证

---

## 🚀 快速选择指南

### 我想要...

| 需求 | 使用配置 | 启动命令 | 优势 |
|------|---------|---------|------|
| **5 分钟快速体验** | h2 | `mvn spring-boot:run -Dspring.profiles.active=h2` | 无需数据库，开箱即用 |
| **本地开发** | 默认（application.yml） | `mvn spring-boot:run` | 功能完整，配置灵活 |
| **生产部署** | mysql | `mvn spring-boot:run -Dspring.profiles.active=mysql` | 性能优化，日志少 |
| **测试 MongoDB** | mongodb | `mvn spring-boot:run -Dspring.profiles.active=mongodb` | 完整 MongoDB 支持 |
| **执行数据迁移** | migration | `mvn spring-boot:run -Dspring.profiles.active=migration` | 双数据源，优化迁移 |

---

## 📊 配置对比表

```
配置项                      application.yml  h2    mysql  mongodb  migration
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
MySQL 支持                  ✓                ✓     ✓      ✗        ✓
MongoDB 支持               ✓(可选)           ✗     ✗      ✓        ✓
H2 支持                    ✗                ✓     ✗      ✗        ✗
双数据源                    ✗                ✗     ✗      ✗        ✓
日志级别                   DEBUG            DEBUG  WARN   INFO     DEBUG
连接池大小(MySQL)          20               10    30     -        40
Batch Size               20               10    20     -        100
DDL 自动执行              update           create validate validate  validate
显示 SQL                  true             true  false  false    false
H2 Web 控制台             ✗                ✓     ✗      ✗        ✗
Prometheus 支持           ✗                ✗     ✓      ✗        ✓
```

---

## 🔄 配置文件内容概览

### 通用配置部分（所有文件都包含）
```yaml
spring:
  application:
    name: spring-boot-101-statemachine

server:
  port: 8080
  servlet:
    context-path: /

logging:
  level:
    root: [根据环境]
    com.example.springboot: [根据环境]
  file:
    name: logs/spring-boot-101.log
```

### 数据源特定部分（根据环境不同）
```yaml
# 默认/MySQL 环境
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/spring_boot_101
    username: root
    password: '123456'

# H2 环境
spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=MySQL
    driver-class-name: org.h2.Driver
  h2:
    console:
      enabled: true
      path: /h2-console

# MongoDB 环境
spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/spring_boot_101
      database: spring_boot_101
```

---

## 💡 常见配置修改

### 修改数据库连接地址
```yaml
# MySQL
spring:
  datasource:
    url: jdbc:mysql://your-host:3306/your-db
    username: your-user
    password: your-password

# MongoDB
spring:
  data:
    mongodb:
      uri: mongodb://your-host:27017/your-db
```

### 修改应用端口
```yaml
server:
  port: 9090
```

### 修改日志级别
```yaml
logging:
  level:
    com.example.springboot: DEBUG
    org.springframework: INFO
    org.hibernate: WARN
```

### 修改连接池大小
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 50
      minimum-idle: 10
```

### 修改日志输出文件
```yaml
logging:
  file:
    name: /var/log/app.log
    max-size: 100MB
    max-history: 30
```

---

## 🎯 环境特定的启动和配置

### 开发环境
```bash
# 方式 1：使用 H2（推荐，无依赖）
mvn spring-boot:run -Dspring.profiles.active=h2

# 方式 2：使用默认配置（需要 MySQL）
mvn spring-boot:run

# 访问应用
curl http://localhost:8080/api/orders/health

# H2 Web 控制台（仅 h2 profile）
http://localhost:8080/h2-console
```

### 生产环境
```bash
# 编译并打包
mvn clean package

# 启动（指定 MySQL 配置）
java -jar target/spring-boot-101-1.0.0.jar --spring.profiles.active=mysql

# 或指定配置文件位置
java -jar target/spring-boot-101-1.0.0.jar \
  --spring.config.location=file:/etc/config/application-mysql.yml
```

### 数据迁移
```bash
# 启动迁移模式（需要 MySQL 和 MongoDB 都在运行）
mvn spring-boot:run -Dspring.profiles.active=migration

# 调用迁移 API
curl http://localhost:8080/api/orders/migration/progress
curl -X POST http://localhost:8080/api/orders/migration/migrate-all
```

---

## 📝 配置优先级

Spring Boot 配置优先级（从高到低）：

1. **命令行参数**
   ```bash
   java -jar app.jar --spring.datasource.url=jdbc:mysql://...
   ```

2. **环境变量**
   ```bash
   export SPRING_DATASOURCE_URL=jdbc:mysql://...
   export SPRING_DATASOURCE_PASSWORD=secret
   ```

3. **Profile 特定配置文件**
   ```
   application-{profile}.yml / .properties
   ```

4. **通用配置文件**
   ```
   application.yml / .properties
   ```

---

## 🔍 常见问题

### Q1: 如何验证当前使用的配置？
A: 查看应用启动日志，会显示 "The following profiles are active: ..."

### Q2: H2 数据库的数据在哪里保存？
A: H2 是内存数据库，数据保存在内存中。应用关闭时数据丢失。

### Q3: 如何在 IDE 中启用特定 profile？
A: 
- IntelliJ IDEA: Run → Edit Configurations → VM options: `-Dspring.profiles.active=h2`
- Eclipse: Run → Run Configurations → Arguments: `-Dspring.profiles.active=h2`

### Q4: 迁移性能不理想，怎么优化？
A:
```yaml
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 200  # 增大批量大小
  datasource:
    hikari:
      maximum-pool-size: 60  # 增大连接池
```

### Q5: 如何在生产环境中隐藏密码？
A: 使用环境变量或密钥管理服务
```bash
export SPRING_DATASOURCE_PASSWORD=your-secret-password
# 配置文件中
password: ${SPRING_DATASOURCE_PASSWORD}
```

---

## ✅ Properties 到 YAML 的迁移清单

如果你还有 application.properties 文件，可以：

- [x] 备份原始 application.properties
- [x] 创建 application.yml（使用 YAML 语法）
- [x] 创建 application-h2.yml（测试配置）
- [x] 创建 application-mysql.yml（生产配置）
- [x] 创建 application-mongodb.yml（MongoDB 配置）
- [x] 创建 application-migration.yml（迁移配置）
- [x] 测试各个配置是否正常工作
- [x] 更新文档和说明
- [x] 提交到版本控制系统

---

## 📚 相关文档

- **README.md** - 项目概览
- **QUICK_START.md** - 快速开始指南
- **MIGRATION_GUIDE.md** - 详细迁移指南
- **YAML_USAGE_GUIDE.txt** - YAML 使用详细指南

---

## 🎓 YAML 语法速查

```yaml
# 字符串
name: Spring Boot

# 数字
port: 8080
timeout: 20000

# 布尔值
enabled: true
disabled: false

# 列表
profiles:
  - h2
  - mysql
  - mongodb

# 嵌套对象
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/db
    username: root
    password: '123456'

# 特殊值
password: null
empty: ''

# 多行字符串
description: |
  This is a long
  description that spans
  multiple lines
```

---

## 🚀 推荐的使用流程

### 第一次使用
1. 使用 h2 profile 快速体验
2. 阅读 QUICK_START.md
3. 运行 test_api.sh 测试 API
4. 浏览代码，理解架构

### 本地开发
1. 启动 MySQL（或使用 Docker）
2. 使用默认 application.yml 配置
3. `mvn spring-boot:run`
4. 修改代码，热部署测试

### 准备生产
1. 使用 application-mysql.yml
2. 修改数据库连接信息
3. `mvn clean package`
4. 在目标环境测试

### 执行迁移
1. 确保 MongoDB 和 MySQL 都在运行
2. 使用 application-migration.yml
3. 调用迁移 API
4. 验证数据一致性
5. 切换到 application-mysql.yml

---

## 💪 性能调优建议

### 开发环境（h2）
```yaml
spring.jpa.show-sql: true
logging.level.root: DEBUG
spring.datasource.hikari.maximum-pool-size: 10
```

### 测试环境（mysql）
```yaml
spring.jpa.show-sql: false
logging.level.root: INFO
spring.datasource.hikari.maximum-pool-size: 20
spring.jpa.properties.hibernate.jdbc.batch_size: 50
```

### 生产环境（mysql）
```yaml
spring.jpa.show-sql: false
logging.level.root: WARN
spring.datasource.hikari.maximum-pool-size: 40
spring.jpa.properties.hibernate.jdbc.batch_size: 100
server.compression.enabled: true
```

---

## 📞 获取帮助

- 查看 YAML_USAGE_GUIDE.txt 获取详细说明
- 检查启动日志确认 profile 激活
- 运行 `mvn spring-boot:run --help` 查看可用选项
- 参考 Spring Boot 官方文档: https://spring.io/projects/spring-boot

---

**最后更新**: 2024 年

**YAML 配置文件总数**: 5 个（application.yml + 4 个 profile 特定配置）

**总配置行数**: 700+ 行

**祝你使用愉快！** 🎉