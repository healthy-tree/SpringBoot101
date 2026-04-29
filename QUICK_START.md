# Spring Boot 101 - 快速开始指南

## 项目概述

本项目是一个完整的 Spring Boot 学习项目，集成了以下核心功能：

- 🎯 **Spring Statemachine** - 订单状态机管理
- 🗄️ **多数据源支持** - MySQL、MongoDB、H2 三种数据库
- 🔄 **数据迁移工具** - 零停机从 MongoDB 迁移到 MySQL
- 📦 **JPA 解耦架构** - 灵活的数据持久化层
- 🚀 **RESTful API** - 完整的订单管理 API

---

## 5 分钟快速启动

### 环境要求

```
- Java 17+
- Maven 3.6+
- MySQL 5.7+ （可选，演示可用 H2）
```

### 最快启动方式（使用 H2 内存数据库）

**1. 克隆项目**
```bash
cd SpringBoot101
```

**2. 修改配置使用 H2**

编辑 `src/main/resources/application.properties`，找到以下行并取消注释：

```properties
# H2 内存数据库配置（仅用于测试）
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

并注释掉 MySQL 配置部分。

**3. 启动应用**

```bash
mvn spring-boot:run
```

**4. 测试 API**

```bash
# 健康检查
curl http://localhost:8080/api/orders/health

# 创建订单
curl -X POST "http://localhost:8080/api/orders?customerId=CUST001&customerName=张三&amount=99.99&description=购买商品"

# 查询订单
curl http://localhost:8080/api/orders/customer/CUST001
```

**5. 访问 H2 控制台（可选）**

打开浏览器访问：http://localhost:8080/h2-console

---

## 完整启动方式（使用 MySQL）

### Step 1: MySQL 数据库准备

```sql
-- 创建数据库
CREATE DATABASE spring_boot_101 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 创建用户（可选）
CREATE USER 'springboot'@'localhost' IDENTIFIED BY 'password123';
GRANT ALL PRIVILEGES ON spring_boot_101.* TO 'springboot'@'localhost';
FLUSH PRIVILEGES;
```

### Step 2: 配置数据库连接

编辑 `src/main/resources/application.properties`：

```properties
# MySQL 配置
spring.datasource.url=jdbc:mysql://localhost:3306/spring_boot_101?useSSL=false&serverTimezone=UTC
spring.datasource.username=springboot
spring.datasource.password=password123
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA 自动建表
spring.jpa.hibernate.ddl-auto=update
```

### Step 3: 编译和运行

```bash
# 清理和编译
mvn clean compile

# 运行应用
mvn spring-boot:run

# 或打包后运行
mvn clean package
java -jar target/spring-boot-101-1.0.0.jar
```

### Step 4: 验证启动成功

```bash
# 健康检查
curl http://localhost:8080/api/orders/health

# 返回示例：
# {
#   "success": true,
#   "status": "UP",
#   "message": "Order API is running"
# }
```

---

## API 快速体验

### 1. 创建订单

```bash
curl -X POST "http://localhost:8080/api/orders?customerId=CUST001&customerName=张三&amount=99.99&description=购买商品"

# 返回示例：
# {
#   "success": true,
#   "message": "订单创建成功",
#   "data": {
#     "id": 1,
#     "orderNumber": "ORD-1234567890-ABC123",
#     "customerId": "CUST001",
#     "customerName": "张三",
#     "amount": 99.99,
#     "status": "PENDING_PAYMENT",
#     ...
#   }
# }
```

保存返回的 `orderNumber`，用于后续操作。

### 2. 查询订单

```bash
# 按订单号查询
curl http://localhost:8080/api/orders/number/ORD-xxx

# 按客户查询
curl http://localhost:8080/api/orders/customer/CUST001

# 按状态查询
curl http://localhost:8080/api/orders/status/PENDING_PAYMENT
```

### 3. 订单状态流转

```bash
# 支付订单
curl -X POST http://localhost:8080/api/orders/ORD-xxx/pay

# 准备订单
curl -X POST http://localhost:8080/api/orders/ORD-xxx/prepare

# 发货
curl -X POST http://localhost:8080/api/orders/ORD-xxx/ship

# 签收
curl -X POST http://localhost:8080/api/orders/ORD-xxx/deliver

# 或取消订单
curl -X POST http://localhost:8080/api/orders/ORD-xxx/cancel?reason=客户要求取消
```

### 4. 查询统计

```bash
# 客户的订单总金额
curl http://localhost:8080/api/orders/customer/CUST001/total-amount

# 客户的已支付金额
curl http://localhost:8080/api/orders/customer/CUST001/paid-amount

# 客户的订单数量
curl http://localhost:8080/api/orders/customer/CUST001/count
```

---

## MongoDB 到 MySQL 迁移快速体验

### 前置条件

需要同时启用 MongoDB 和 MySQL：

```bash
# 启动 MongoDB（如果没有，可跳过）
mongod --dbpath /path/to/mongodb/data

# 或使用 Docker
docker run -d -p 27017:27017 --name mongodb mongo:latest
```

### 启用迁移模式

编辑 `src/main/resources/application.properties`：

```properties
# 启用双数据源模式
datasource.enable-migration=true
datasource.type=mysql
datasource.primary=mysql

# 配置 MySQL
spring.datasource.url=jdbc:mysql://localhost:3306/spring_boot_101
spring.datasource.username=root
spring.datasource.password=123456

# 配置 MongoDB
spring.data.mongodb.uri=mongodb://localhost:27017/spring_boot_101
```

### 迁移步骤

```bash
# 1. 获取迁移进度
curl http://localhost:8080/api/orders/migration/progress

# 2. 执行全量迁移
curl -X POST http://localhost:8080/api/orders/migration/migrate-all

# 3. 验证迁移数据
curl http://localhost:8080/api/orders/migration/validate

# 4. 如果需要回滚
curl -X POST http://localhost:8080/api/orders/migration/rollback
```

### 迁移完成后

```properties
# 关闭双数据源模式
datasource.enable-migration=false
datasource.type=mysql
```

然后重新启动应用。

---

## 项目结构

```
SpringBoot101/
├── src/
│   ├── main/
│   │   ├── java/com/example/springboot/
│   │   │   ├── Application.java                    # 启动类
│   │   │   ├── controller/
│   │   │   │   ├── OrderController.java            # 订单 API 控制器
│   │   │   │   └── HelloController.java            # 示例控制器
│   │   │   ├── service/
│   │   │   │   ├── OrderService.java              # 订单业务逻辑
│   │   │   │   └── MigrationService.java          # 迁移服务
│   │   │   ├── model/
│   │   │   │   ├── Order.java                     # JPA 订单实体
│   │   │   │   └── OrderDocument.java             # MongoDB 订单文档
│   │   │   ├── repository/
│   │   │   │   ├── jpa/OrderRepository.java       # JPA 数据访问
│   │   │   │   └── mongo/OrderDocumentRepository  # MongoDB 数据访问
│   │   │   ├── statemachine/
│   │   │   │   ├── OrderState.java                # 订单状态枚举
│   │   │   │   ├── OrderEvent.java                # 订单事件枚举
│   │   │   │   └── StateMachineConfig.java        # 状态机配置
│   │   │   ├── config/
│   │   │   │   └── DataSourceConfig.java          # 多数据源配置
│   │   │   └── migration/
│   │   └── resources/
│   │       └── application.properties             # 应用配置
│   └── test/
│       └── java/com/example/springboot/           # 测试代码
├── pom.xml                                        # Maven 配置
├── MIGRATION_GUIDE.md                             # 迁移完整指南
├── QUICK_START.md                                 # 本文件
└── README.md                                      # 项目说明
```

---

## 常用命令

```bash
# 清理编译
mvn clean

# 编译项目
mvn compile

# 运行测试
mvn test

# 打包应用
mvn package

# 跳过测试打包
mvn package -DskipTests

# 直接运行
mvn spring-boot:run

# 查看依赖树
mvn dependency:tree

# 更新依赖
mvn dependency:resolve
```

---

## 日志查看

应用日志保存在 `logs/spring-boot-101.log`

```bash
# 实时查看日志
tail -f logs/spring-boot-101.log

# 搜索错误日志
grep ERROR logs/spring-boot-101.log

# 搜索迁移日志
grep MIGRATION logs/spring-boot-101.log
```

---

## 订单状态流程图

```
                    ┌─────────────────────┐
                    │ PENDING_PAYMENT     │
                    │ (待支付)             │
                    └──────────┬──────────┘
                               │
                    (支付 PAY 事件)
                               │
                    ┌──────────▼──────────┐
                    │ PAID                │
                    │ (已支付)             │
                    └──────────┬──────────┘
                               │
                  (准备 PREPARE 事件)
                               │
                    ┌──────────▼──────────┐
                    │ PREPARING           │
                    │ (准备中)             │
                    └──────────┬──────────┘
                               │
                    (发货 SHIP 事件)
                               │
                    ┌──────────▼──────────┐
                    │ SHIPPED             │
                    │ (已发货)             │
                    └──────────┬──────────┘
                               │
                  (签收 DELIVER 事件)
                               │
                    ┌──────────▼──────────┐
                    │ DELIVERED           │
                    │ (已签收)             │
                    │ [最终状态]          │
                    └─────────────────────┘

   取消流程：
   ┌────────────────────────────────────┐
   │ PENDING_PAYMENT / PAID / PREPARING  │
   │        (取消 CANCEL 事件)           │
   │              ▼                      │
   │         CANCELLED                   │
   │         (已取消)                    │
   │              ▼                      │
   │   (退款 REFUND 事件)                │
   │              ▼                      │
   │         REFUNDED                    │
   │         (已退款)                    │
   │       [最终状态]                    │
   └────────────────────────────────────┘
```

---

## 性能优化建议

### 连接池调整

如果频繁遇到连接超时，可调整 `application.properties`：

```properties
spring.datasource.hikari.maximum-pool-size=30
spring.datasource.hikari.minimum-idle=10
spring.datasource.hikari.connection-timeout=30000
```

### 批量操作优化

迁移大量数据时，可调整批量大小：

在 `MigrationService.java` 中修改：

```java
private static final int BATCH_SIZE = 200;  // 增大批量大小
```

### 查询性能

使用合适的分页参数：

```bash
# 获取第一页，每页 20 条
curl "http://localhost:8080/api/orders/customer/CUST001?page=0&size=20&sort=createdAt,desc"
```

---

## 故障排查

### 问题 1：无法连接 MySQL

```bash
# 检查 MySQL 是否运行
mysql -u root -p -e "SELECT 1;"

# 检查配置中的主机、端口、用户名、密码
# 确保数据库已创建
```

### 问题 2：无法连接 MongoDB

```bash
# 检查 MongoDB 是否运行
mongo --eval "db.adminCommand('ping')"

# 检查 URI 配置是否正确
# 默认：mongodb://localhost:27017/spring_boot_101
```

### 问题 3：应用启动失败

```bash
# 查看详细错误日志
tail -100 logs/spring-boot-101.log

# 检查端口 8080 是否被占用
lsof -i :8080

# 检查 Java 版本
java -version
```

### 问题 4：迁移过程缓慢

```bash
# 增加内存
export MAVEN_OPTS="-Xmx2G"
mvn spring-boot:run

# 调整数据库连接池大小
# 在 application.properties 中增大 maximum-pool-size
```

---

## 下一步学习

1. **阅读迁移指南** - 详见 `MIGRATION_GUIDE.md`
2. **深入了解状态机** - 修改 `StateMachineConfig.java` 添加自定义状态
3. **扩展 API** - 在 `OrderController.java` 中添加新的业务逻辑
4. **性能优化** - 研究 Hibernate、JPA 和数据库优化
5. **单元测试** - 在 `src/test` 中添加测试用例

---

## 获取帮助

### 官方文档

- [Spring Boot 官方文档](https://spring.io/projects/spring-boot)
- [Spring Data JPA](https://spring.io/projects/spring-data-jpa)
- [Spring Statemachine](https://spring.io/projects/spring-statemachine)
- [MongoDB 中文文档](https://docs.mongodb.com/manual/zh/)

### 本项目文档

- `README.md` - 项目概览
- `MIGRATION_GUIDE.md` - 完整迁移指南
- `QUICK_START.md` - 本快速开始指南

---

**祝你学习愉快！** 🚀