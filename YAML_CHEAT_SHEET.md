# Spring Boot 101 - YAML 配置快速参考卡片

## 🚀 一分钟快速开始

### 最快方式（无需数据库）
```bash
mvn spring-boot:run -Dspring.profiles.active=h2
# 然后访问 http://localhost:8080/api/orders/health
# H2 Web 控制台: http://localhost:8080/h2-console (用户名: sa, 密码: 留空)
```

### 默认方式（需要 MySQL）
```bash
mvn spring-boot:run
# 修改 application.yml 中的数据库连接信息
```

## 📂 配置文件一览

| 文件 | 用途 | 启动命令 | 数据库 |
|------|------|---------|--------|
| application.yml | 默认/开发 | `mvn spring-boot:run` | MySQL |
| application-h2.yml | 快速测试 | `-Dspring.profiles.active=h2` | H2 |
| application-mysql.yml | 生产环境 | `-Dspring.profiles.active=mysql` | MySQL |
| application-mongodb.yml | MongoDB | `-Dspring.profiles.active=mongodb` | MongoDB |
| application-migration.yml | 数据迁移 | `-Dspring.profiles.active=migration` | MySQL+MongoDB |

## ⚙️ 常用配置修改

### 修改 MySQL 连接
```yaml
spring:
  datasource:
    url: jdbc:mysql://your-host:3306/your-db?useSSL=false&serverTimezone=UTC
    username: your-user
    password: 'your-password'
```

### 修改 MongoDB 连接
```yaml
spring:
  data:
    mongodb:
      uri: mongodb://your-host:27017/your-db
      database: your-db
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
    root: INFO
    com.example.springboot: DEBUG
    org.springframework: WARN
    org.hibernate: WARN
```

### 修改连接池大小
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 50      # 最大连接数
      minimum-idle: 10           # 最小空闲连接
      connection-timeout: 30000  # 连接超时（毫秒）
```

### 修改日志文件位置
```yaml
logging:
  file:
    name: /var/log/app.log
    max-size: 100MB
    max-history: 30
```

### 迁移性能优化
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 60      # 增大连接池
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 200        # 增大批量大小
```

## 🎯 三种数据源启动方式

### 仅 MySQL（生产环境）
```bash
mvn spring-boot:run -Dspring.profiles.active=mysql
# 配置: application-mysql.yml
```

### 仅 MongoDB（原始环境）
```bash
mvn spring-boot:run -Dspring.profiles.active=mongodb
# 配置: application-mongodb.yml
```

### MySQL + MongoDB（数据迁移）
```bash
mvn spring-boot:run -Dspring.profiles.active=migration
# 配置: application-migration.yml
# 调用 API: /api/orders/migration/*
```

## 🔑 关键配置项说明

### datasource.enable-migration
- `false` - 仅使用一个数据源（默认）
- `true` - 启用双数据源模式，用于 MongoDB→MySQL 迁移

### datasource.type
- `mysql` - 使用 MySQL 作为主数据源
- `mongodb` - 使用 MongoDB 作为主数据源

### spring.jpa.hibernate.ddl-auto
- `create-drop` - 每次启动创建，关闭时删除（H2 测试）
- `update` - 启动时更新表结构（开发）
- `validate` - 仅验证，不修改（生产）

### 日志级别
- `DEBUG` - 详细调试信息（开发）
- `INFO` - 一般信息
- `WARN` - 警告信息
- `ERROR` - 错误信息

## 📋 YAML 语法速查

```yaml
# 字符串
name: Spring Boot
description: 'value with spaces'
path: /var/log/app.log

# 数字
port: 8080
timeout: 20000
price: 99.99

# 布尔值
enabled: true
disabled: false

# 列表/数组
profiles:
  - h2
  - mysql
  - mongodb

expose:
  include:
    - health
    - info
    - metrics

# 对象/嵌套
spring:
  datasource:
    url: jdbc:mysql://...
    username: root
    password: '123456'
  jpa:
    show-sql: true

# 环境变量引用
password: ${DB_PASSWORD}

# 多行字符串
description: |
  Line 1
  Line 2
  Line 3

# 空值
value: null
empty: ''
```

## 🔍 常见问题速解

### Q: 启动时显示"Could not get a resource from the pool"
A: 数据库连接失败
- 检查数据库是否运行
- 检查连接字符串
- 减小 maximum-pool-size

### Q: 配置文件没有被读取
A: Profile 可能未激活
- 检查启动日志中 "The following profiles are active"
- 确认 `-Dspring.profiles.active=name` 正确

### Q: H2 数据库看不到表
A: 这是正常的，应用每次启动都会重建数据库
- 使用 `ddl-auto: create-drop` 配置

### Q: 迁移速度慢
A: 调整以下配置
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 60
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 200
```

### Q: 如何隐藏密码？
A: 使用环境变量
```yaml
password: ${SPRING_DATASOURCE_PASSWORD}
```
然后启动：
```bash
export SPRING_DATASOURCE_PASSWORD=your-secret
mvn spring-boot:run
```

## 📊 环境对比速查

| 需求 | 推荐配置 | 启动命令 |
|------|---------|---------|
| 快速体验（5分钟） | h2 | `-Dspring.profiles.active=h2` |
| 本地开发 | 默认 | `mvn spring-boot:run` |
| 生产部署 | mysql | `-Dspring.profiles.active=mysql` |
| 测试 MongoDB | mongodb | `-Dspring.profiles.active=mongodb` |
| 数据迁移 | migration | `-Dspring.profiles.active=migration` |

## 🛠️ 常用命令

```bash
# 使用特定 profile 启动
mvn spring-boot:run -Dspring.profiles.active=h2

# 打包应用
mvn clean package

# 运行打包的 JAR
java -jar target/spring-boot-101-1.0.0.jar --spring.profiles.active=mysql

# 使用环境变量启动
export SPRING_PROFILES_ACTIVE=h2
mvn spring-boot:run

# 查看帮助
mvn spring-boot:run --help

# 运行自动化测试
bash test_api.sh

# 详细的测试输出
bash test_api.sh --verbose

# 测试特定 URL
bash test_api.sh --url http://example.com:8080
```

## 🎓 IDE 配置（IntelliJ IDEA）

1. Run → Edit Configurations...
2. 找到 "Spring Boot" 配置
3. 在 "VM options" 中输入：
   ```
   -Dspring.profiles.active=h2
   ```
4. 点击 "Run"

## 📈 性能配置预设

### 开发（开发者机器）
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
  jpa:
    show-sql: true
logging:
  level:
    root: DEBUG
```

### 生产（1-5 并发用户）
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
  jpa:
    show-sql: false
logging:
  level:
    root: WARN
```

### 生产（5-50 并发用户）
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 40
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 100
logging:
  level:
    root: WARN
server:
  compression:
    enabled: true
```

### 迁移（大数据量）
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 60
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 200
logging:
  level:
    com.example.springboot.service.MigrationService: DEBUG
```

## 📞 获取帮助

- 详细指南: `YAML_USAGE_GUIDE.txt`
- 配置总结: `YAML_CONFIG_SUMMARY.md`
- 快速开始: `QUICK_START.md`
- 迁移指南: `MIGRATION_GUIDE.md`
- 官方文档: https://spring.io/projects/spring-boot

---

**提示**: 最常用的三个命令
```bash
# 快速测试（无需数据库）
mvn spring-boot:run -Dspring.profiles.active=h2

# 本地开发（需要 MySQL）
mvn spring-boot:run

# 生产部署
java -jar app.jar --spring.profiles.active=mysql
```

---

**最后更新**: 2024 年