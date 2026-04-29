# MongoDB 到 MySQL 迁移指南

## 目录
1. [概述](#概述)
2. [架构设计](#架构设计)
3. [前置条件](#前置条件)
4. [迁移策略](#迁移策略)
5. [详细迁移步骤](#详细迁移步骤)
6. [API 使用指南](#api-使用指南)
7. [数据验证](#数据验证)
8. [回滚方案](#回滚方案)
9. [性能优化](#性能优化)
10. [常见问题](#常见问题)
11. [最佳实践](#最佳实践)

---

## 概述

本项目基于 Spring Boot 和 Spring Statemachine，实现了订单管理系统。原始系统使用 MongoDB 存储订单数据，现需要迁移到 MySQL。

### 迁移的主要目标

- ✅ 零停机迁移（双数据源并行运行）
- ✅ 数据完整性保证（支持数据验证）
- ✅ 支持灰度迁移（按客户、按时间范围）
- ✅ 支持快速回滚
- ✅ 保持业务连续性

### 核心数据结构

```
订单实体（Order/OrderDocument）
├── 订单号 (orderNumber) - 业务唯一标识
├── 客户ID (customerId)
├── 订单金额 (amount)
├── 订单状态 (status) - 使用状态机管理
├── 时间戳
│   ├── 创建时间 (createdAt)
│   ├── 更新时间 (updatedAt)
│   ├── 支付时间 (paidAt)
│   ├── 发货时间 (shippedAt)
│   └── 签收时间 (deliveredAt)
└── 迁移跟踪字段
    ├── 数据源 (dataSource)
    └── 迁移时间 (migratedAt)
```

---

## 架构设计

### JPA 解耦架构

项目采用 JPA 作为数据访问层的抽象，支持多种数据库后端：

```
┌─────────────────────────────────────────┐
│        OrderService（业务逻辑层）       │
│      - 订单创建、支付、发货等操作      │
│      - 状态机集成                      │
└──────────────┬──────────────────────────┘
               │
        ┌──────┴──────┐
        │             │
┌───────▼─────────┐  ┌────────▼────────┐
│ OrderRepository │  │MigrationService │
│   (JPA/MySQL)   │  │   (数据迁移)    │
└───────┬─────────┘  └────────┬────────┘
        │                     │
    ┌───┴──────────────┬──────┴──────┐
    │                  │             │
┌───▼─────────┐  ┌────▼──────┐  ┌──▼────────┐
│   MySQL     │  │ MongoDB   │  │  H2(测试) │
│  (生产环境)  │  │(过渡环境)  │  │  (开发)   │
└─────────────┘  └───────────┘  └───────────┘
```

### 多数据源支持

项目包含三种数据源配置模式：

#### 1. MySQL 模式（生产默认）
```
datasource.type=mysql
datasource.primary=mysql
datasource.enable-migration=false
```
- 仅启用 MySQL
- JPA Repository 生效
- 用于迁移完成后的生产环境

#### 2. MongoDB 模式（原始环境）
```
datasource.type=mongodb
datasource.primary=mongodb
datasource.enable-migration=false
```
- 仅启用 MongoDB
- MongoDB Repository 生效
- 用于迁移前的原始环境

#### 3. 双数据源模式（迁移过程中）
```
datasource.type=mysql
datasource.primary=mysql
datasource.enable-migration=true
```
- 同时启用 MySQL 和 MongoDB
- 两个数据源都可访问
- 用于迁移过程中的数据验证和同步

---

## 前置条件

### 系统环境要求

```
- Java 17+
- Maven 3.6+
- MySQL 5.7+ (或 8.0+)
- MongoDB 4.0+ (迁移期间需要)
```

### 数据库准备

#### MySQL 数据库创建
```sql
-- 创建数据库
CREATE DATABASE spring_boot_101 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

-- 创建用户（可选）
CREATE USER 'springboot'@'localhost' IDENTIFIED BY 'password123';
GRANT ALL PRIVILEGES ON spring_boot_101.* TO 'springboot'@'localhost';
FLUSH PRIVILEGES;

-- 使用 JPA 自动创建表或手动执行以下 SQL
```

#### MySQL 表结构
```sql
CREATE TABLE IF NOT EXISTS orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_number VARCHAR(32) NOT NULL UNIQUE,
    customer_id VARCHAR(50) NOT NULL,
    customer_name VARCHAR(100),
    amount DECIMAL(10, 2) NOT NULL,
    description VARCHAR(500),
    status VARCHAR(32) NOT NULL,
    remarks VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    paid_at TIMESTAMP NULL,
    shipped_at TIMESTAMP NULL,
    delivered_at TIMESTAMP NULL,
    cancelled_at TIMESTAMP NULL,
    version BIGINT DEFAULT 0,
    
    INDEX idx_order_number (order_number),
    INDEX idx_customer_id (customer_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

#### MongoDB 集合准备
```javascript
// 创建数据库和集合
use spring_boot_101

// 创建索引
db.orders.createIndex({ "orderNumber": 1 }, { unique: true })
db.orders.createIndex({ "customerId": 1 })
db.orders.createIndex({ "status": 1 })
db.orders.createIndex({ "createdAt": -1 })
db.orders.createIndex({ "customerId": 1, "status": 1 })
```

### 依赖配置验证

确保 `pom.xml` 中包含以下依赖：
- spring-boot-starter-data-jpa
- spring-boot-starter-data-mongodb
- mysql-connector-java
- spring-statemachine-starter

---

## 迁移策略

### 策略对比

| 策略 | 适用场景 | 风险 | 优点 |
|-----|--------|------|------|
| **全量迁移** | 小数据量<5万条 | 一次性迁移失败风险较大 | 快速完成，无增量处理 |
| **增量迁移** | 大数据量>5万条 | 可能遗漏新增数据 | 可分批处理，降低风险 |
| **灰度迁移** | 生产环境 | 需要更复杂的控制逻辑 | 可验证数据准确性 |
| **双写迁移** | 关键业务 | 需要修改代码 | 零停机，高可用 |

### 推荐迁移路径

```
Phase 1: 准备阶段
├─ 环境检查和数据备份
├─ 启用双数据源模式
└─ 部署新版本应用

Phase 2: 预迁移验证
├─ 灰度迁移测试数据
├─ 数据一致性验证
└─ 性能基准测试

Phase 3: 正式迁移
├─ 全量或增量迁移
├─ 实时监控
└─ 增量同步处理

Phase 4: 验证和切换
├─ 全量数据验证
├─ 应用配置切换
└─ 监控和优化

Phase 5: 清理和收尾
├─ 保留 MongoDB 备份
├─ 禁用双数据源模式
└─ 性能优化
```

---

## 详细迁移步骤

### Step 1: 环境准备

#### 1.1 数据备份
```bash
# MongoDB 备份
mongodump --db spring_boot_101 --out ./mongodb_backup

# MySQL 备份（如果已有数据）
mysqldump -u root -p spring_boot_101 > mysql_backup.sql
```

#### 1.2 配置应用为双数据源模式

编辑 `application.properties`：
```properties
# 启用双数据源模式
datasource.enable-migration=true
datasource.type=mysql
datasource.primary=mysql

# MySQL 配置
spring.datasource.url=jdbc:mysql://localhost:3306/spring_boot_101?useSSL=false&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=123456

# MongoDB 配置
spring.data.mongodb.uri=mongodb://localhost:27017/spring_boot_101
spring.data.mongodb.database=spring_boot_101
```

#### 1.3 重新编译和部署
```bash
# 清理和编译
mvn clean compile

# 运行应用
mvn spring-boot:run
```

### Step 2: 数据迁移

#### 2.1 获取迁移进度
```bash
curl -X GET http://localhost:8080/api/orders/migration/progress

# 返回示例：
# {
#   "totalRecords": 10000,
#   "migratedRecords": 0,
#   "pendingRecords": 10000,
#   "progressPercentage": 0
# }
```

#### 2.2 执行灰度迁移（推荐）

首先迁移某个特定客户的数据进行测试：
```bash
CUSTOMER_ID="CUST001"
curl -X POST http://localhost:8080/api/orders/migration/migrate-by-customer/${CUSTOMER_ID}

# 返回示例：
# {
#   "success": true,
#   "message": "客户迁移完成",
#   "data": {
#     "totalRecords": 500,
#     "successCount": 500,
#     "failureCount": 0,
#     "skipCount": 0,
#     "startTime": "2024-01-15T10:00:00",
#     "endTime": "2024-01-15T10:05:00",
#     "errors": []
#   }
# }
```

#### 2.3 执行全量迁移（大数据量时分批处理）

```bash
# 执行全量迁移
curl -X POST http://localhost:8080/api/orders/migration/migrate-all

# 或者执行增量迁移（推荐用于大数据量）
curl -X POST http://localhost:8080/api/orders/migration/migrate-incremental
```

#### 2.4 监控迁移进度

```bash
# 定期查询进度
watch -n 5 'curl -s http://localhost:8080/api/orders/migration/progress | jq'

# 或使用脚本监控
#!/bin/bash
while true; do
  echo "$(date): 获取迁移进度..."
  curl -s http://localhost:8080/api/orders/migration/progress | jq '.data'
  sleep 30
done
```

### Step 3: 数据验证

#### 3.1 执行数据验证
```bash
curl -X GET http://localhost:8080/api/orders/migration/validate

# 返回示例：
# {
#   "success": true,
#   "message": "迁移验证完成",
#   "data": {
#     "totalRecords": 10000,
#     "successCount": 10000,
#     "failureCount": 0,
#     "skipCount": 0,
#     "errors": []
#   }
# }
```

#### 3.2 数据一致性检查

```sql
-- 检查订单总数
SELECT COUNT(*) as total_orders FROM orders;

-- 检查各状态订单数
SELECT status, COUNT(*) as count FROM orders GROUP BY status;

-- 检查金额汇总
SELECT SUM(amount) as total_amount FROM orders;

-- 检查客户订单分布
SELECT customer_id, COUNT(*) as order_count FROM orders GROUP BY customer_id;
```

#### 3.3 业务验证

```bash
# 测试订单操作
ORDER_NUMBER="ORD-xxx"

# 获取订单详情
curl -X GET http://localhost:8080/api/orders/number/${ORDER_NUMBER}

# 测试订单状态转换
curl -X POST http://localhost:8080/api/orders/${ORDER_NUMBER}/pay

# 验证返回的数据是否正确
```

### Step 4: 配置切换

#### 4.1 关闭双数据源模式
```properties
datasource.enable-migration=false
datasource.type=mysql
datasource.primary=mysql
```

#### 4.2 重新部署应用
```bash
mvn clean package
java -jar target/spring-boot-101-1.0.0.jar
```

#### 4.3 验证应用正常运行
```bash
# 健康检查
curl http://localhost:8080/api/orders/health

# 测试 API
curl http://localhost:8080/api/orders/customer/CUST001
```

---

## API 使用指南

### 订单管理 API

#### 创建订单
```bash
curl -X POST "http://localhost:8080/api/orders?customerId=CUST001&customerName=张三&amount=99.99&description=购买商品"
```

#### 查询订单
```bash
# 按 ID
curl http://localhost:8080/api/orders/1

# 按订单号
curl http://localhost:8080/api/orders/number/ORD-xxx

# 按客户 ID
curl http://localhost:8080/api/orders/customer/CUST001

# 按状态
curl http://localhost:8080/api/orders/status/PENDING_PAYMENT
```

#### 订单状态转换
```bash
# 支付
curl -X POST http://localhost:8080/api/orders/ORD-xxx/pay

# 准备
curl -X POST http://localhost:8080/api/orders/ORD-xxx/prepare

# 发货
curl -X POST http://localhost:8080/api/orders/ORD-xxx/ship

# 签收
curl -X POST http://localhost:8080/api/orders/ORD-xxx/deliver

# 取消
curl -X POST http://localhost:8080/api/orders/ORD-xxx/cancel?reason=客户要求取消

# 退款
curl -X POST http://localhost:8080/api/orders/ORD-xxx/refund
```

### 迁移管理 API

#### 全量迁移
```bash
curl -X POST http://localhost:8080/api/orders/migration/migrate-all
```

#### 增量迁移
```bash
curl -X POST http://localhost:8080/api/orders/migration/migrate-incremental
```

#### 灰度迁移（按客户）
```bash
curl -X POST http://localhost:8080/api/orders/migration/migrate-by-customer/CUST001
```

#### 数据验证
```bash
curl http://localhost:8080/api/orders/migration/validate
```

#### 迁移进度
```bash
curl http://localhost:8080/api/orders/migration/progress
```

#### 回滚迁移
```bash
curl -X POST http://localhost:8080/api/orders/migration/rollback
```

---

## 数据验证

### 验证清单

- [ ] 总记录数一致
- [ ] 各状态订单数一致
- [ ] 金额汇总一致
- [ ] 关键字段值一致
- [ ] 索引创建正确
- [ ] 主键约束正确
- [ ] 时间戳精度一致
- [ ] 业务逻辑测试通过

### 验证脚本示例

```bash
#!/bin/bash

# 连接 MySQL 验证
MYSQL_COUNT=$(mysql -h localhost -u root -p123456 spring_boot_101 -N -e "SELECT COUNT(*) FROM orders;")
MONGODB_COUNT=$(mongo --quiet --eval "use spring_boot_101; db.orders.count();")

echo "MySQL 订单数：$MYSQL_COUNT"
echo "MongoDB 订单数：$MONGODB_COUNT"

if [ "$MYSQL_COUNT" -eq "$MONGODB_COUNT" ]; then
    echo "✓ 记录数一致"
else
    echo "✗ 记录数不一致！"
    exit 1
fi

# 验证金额汇总
MYSQL_AMOUNT=$(mysql -h localhost -u root -p123456 spring_boot_101 -N -e "SELECT COALESCE(SUM(amount), 0) FROM orders;")
MONGODB_AMOUNT=$(mongo --quiet --eval "use spring_boot_101; db.orders.aggregate([{\$group: {_id: null, total: {\$sum: '\$amount'}}}]);")

echo "MySQL 总金额：$MYSQL_AMOUNT"
echo "MongoDB 总金额：$MONGODB_AMOUNT"
```

---

## 回滚方案

### 快速回滚步骤

#### 情况 1：迁移尚未完成

如果迁移过程中发现问题且未完成，可以快速回滚：

```bash
# 1. 调用回滚 API
curl -X POST http://localhost:8080/api/orders/migration/rollback

# 2. 检查回滚结果
curl http://localhost:8080/api/orders/migration/validate

# 3. 恢复为 MongoDB 模式
# 编辑 application.properties
datasource.type=mongodb
datasource.primary=mongodb
datasource.enable-migration=false

# 4. 重新部署应用
mvn clean package
java -jar target/spring-boot-101-1.0.0.jar
```

#### 情况 2：迁移完成后发现问题

如果已经切换到 MySQL 但发现数据问题：

```bash
# 1. 从备份恢复 MongoDB
mongorestore ./mongodb_backup

# 2. 启用双数据源模式
datasource.enable-migration=true

# 3. 重新迁移（数据会覆盖）
curl -X POST http://localhost:8080/api/orders/migration/migrate-all

# 4. 验证和切换
```

#### 情况 3：应用级别回滚

如果需要完全回滚到迁移前：

```bash
# 1. 恢复 MySQL 备份（如果有）
mysql -u root -p123456 spring_boot_101 < mysql_backup.sql

# 2. 恢复应用配置为 MongoDB
datasource.type=mongodb
datasource.enable-migration=false

# 3. 清空 MySQL 数据库
mysql -u root -p123456 spring_boot_101 -e "DROP TABLE orders;"

# 4. 重新部署应用
mvn clean package && java -jar target/spring-boot-101-1.0.0.jar
```

---

## 性能优化

### 迁移性能优化

#### 1. 批量处理优化
```java
// 在 MigrationService 中配置批量大小
private static final int BATCH_SIZE = 100;  // 可根据内存调整

// Hibernate 批量插入优化
spring.jpa.properties.hibernate.jdbc.batch_size=20
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true
```

#### 2. 连接池优化
```properties
# HikariCP 连接池
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=20000
spring.datasource.hikari.idle-timeout=300000
spring.datasource.hikari.max-lifetime=1200000
```

#### 3. 数据库索引优化
```sql
-- 创建必要的索引以加速查询
CREATE INDEX idx_customer_id ON orders(customer_id);
CREATE INDEX idx_status ON orders(status);
CREATE INDEX idx_created_at ON orders(created_at);
CREATE INDEX idx_customer_status ON orders(customer_id, status);
```

#### 4. 迁移脚本优化
```bash
#!/bin/bash
# 分批迁移脚本
for customer_id in $(mongo --quiet --eval "use spring_boot_101; db.orders.distinct('customerId');"); do
    echo "迁移客户: $customer_id"
    curl -X POST http://localhost:8080/api/orders/migration/migrate-by-customer/${customer_id}
    sleep 5  # 避免数据库过载
done
```

### 查询性能优化

```properties
# 启用查询优化
spring.jpa.properties.hibernate.generate_statistics=false
spring.jpa.properties.hibernate.use_sql_comments=true

# 启用查询缓存（可选）
spring.jpa.properties.hibernate.cache.use_second_level_cache=true
```

---

## 常见问题

### Q1: 迁移过程中如何处理新增订单？

**A**: 新增订单会直接写入当前主数据源（MySQL），可定期执行增量迁移同步 MongoDB 中的历史数据。

```bash
# 定期执行增量迁移
curl -X POST http://localhost:8080/api/orders/migration/migrate-incremental
```

### Q2: 迁移失败后数据会重复吗？

**A**: 不会。迁移服务会检查订单号是否已存在，防止重复迁移。

```java
// MigrationService 中的检查逻辑
Optional<Order> existing = orderRepository.findByOrderNumber(document.getOrderNumber());
if (existing.isPresent()) {
    result.skipCount++;
    return;
}
```

### Q3: 如何验证迁移的数据完整性？

**A**: 使用验证 API：

```bash
curl -X GET http://localhost:8080/api/orders/migration/validate

# 检查 failureCount 是否为 0
# 检查 errors 列表是否为空
```

### Q4: MongoDB 和 MySQL 版本有什么要求？

**A**: 
- MySQL 5.7+ 或 8.0+
- MongoDB 4.0+

建议使用最新稳定版本以获得最佳性能。

### Q5: 迁移后 MongoDB 数据是否可以删除？

**A**: 建议保留 MongoDB 数据作为备份，至少保留 30 天。确认数据完全无误后再删除。

```bash
# 生成最终备份
mongodump --db spring_boot_101 --out ./final_mongodb_backup

# 定期检查后再删除
# db.orders.deleteMany({})
```

### Q6: 如何处理大小写敏感的字段？

**A**: MySQL 默认不区分大小写，如需区分，修改 SQL：

```sql
CREATE TABLE orders (
    ...
    order_number VARCHAR(32) COLLATE utf8mb4_bin NOT NULL UNIQUE,
    ...
) COLLATE utf8mb4_unicode_ci;
```

---

## 最佳实践

### 1. 数据备份策略

```bash
# 迁移前备份
mongodump --db spring_boot_101 --out ./backup_before_migration
mysqldump -u root -p spring_boot_101 > backup_mysql_before.sql

# 迁移后备份
mongodump --db spring_boot_101 --out ./backup_after_migration
mysqldump -u root -p spring_boot_101 > backup_mysql_after.sql

# 定期备份
0 2 * * * mongodump --db spring_boot_101 --out ./backups/$(date +\%Y\%m\%d)
```

### 2. 监控和告警

```bash
# 监控迁移进度
watch -n 30 'curl -s http://localhost:8080/api/orders/migration/progress | jq'

# 监控应用日志
tail -f logs/spring-boot-101.log | grep -E "(MIGRATION|ERROR|WARN)"

# 监控数据库性能
# MySQL
SHOW FULL PROCESSLIST;
SHOW INNODB STATUS\G;

# MongoDB
db.currentOp()
```

### 3. 文档记录

迁移过程中记录：
- 迁移开始时间
- 迁移完成时间
- 迁移记录数
- 失败数和跳过数
- 验证结果
- 性能指标

### 4. 灰度验证

```
Day 1: 迁移 10% 的客户（小客户）
  ├─ 验证数据完整性
  ├─ 测试订单操作
  └─ 监控系统性能

Day 2-3: 迁移 30% 的客户（中等客户）
  ├─ 继续验证
  ├─ 扩大范围测试
  └─ 监控稳定性

Day 4: 迁移 100% 的客户（全量）
  ├─ 最终验证
  ├─ 配置切换
  └─ 关闭双数据源
```

### 5. 应急预案

准备应急响应计划：

| 问题 | 响应时间 | 解决方案 |
|-----|--------|--------|
| 迁移进度缓慢 | 立即 | 调整批量大小，减少数据库竞争 |
| 数据不一致 | 1小时 | 停止应用，执行验证，决定回滚 |
| 应用性能下降 | 30分钟 | 检查数据库连接，调整连接池参数 |
| 迁移失败 | 2小时 | 分析日志，修复问题，重新迁移 |

### 6. 清理和收尾

```bash
# 1. 确认迁移完成
curl http://localhost:8080/api/orders/migration/progress

# 2. 关闭双数据源模式
# 编辑 application.properties：datasource.enable-migration=false

# 3. 移除 MongoDB 依赖（可选）
# 从 pom.xml 移除 spring-boot-starter-data-mongodb

# 4. 清理应用日志
rm logs/spring-boot-101.log

# 5. 备档迁移文档和备份
tar -czf migration_backup_$(date +%Y%m%d).tar.gz ./backups/ ./migration_docs/
```

---

## 相关配置文件示例

### application-mysql.properties（仅 MySQL）
```properties
datasource.type=mysql
datasource.enable-migration=false

spring.datasource.url=jdbc:mysql://localhost:3306/spring_boot_101
spring.datasource.username=root
spring.datasource.password=123456
```

### application-mongodb.properties（仅 MongoDB）
```properties
datasource.type=mongodb
datasource.enable-migration=false

spring.data.mongodb.uri=mongodb://localhost:27017/spring_boot_101
```

### application-migration.properties（迁移模式）
```properties
datasource.enable-migration=true
datasource.type=mysql
datasource.primary=mysql

spring.datasource.url=jdbc:mysql://localhost:3306/spring_boot_101
spring.data.mongodb.uri=mongodb://localhost:27017/spring_boot_101
```

---

## 总结

本迁移方案通过：
- ✅ **JPA 抽象层** - 支持多种数据库后端
- ✅ **双数据源模式** - 零停机迁移
- ✅ **灰度迁移策略** - 降低风险
- ✅ **完整验证机制** - 保证数据完整性
- ✅ **快速回滚方案** - 应急保障

确保了从 MongoDB 到 MySQL 的平稳、安全的迁移过程。

---

**迁移者** | **日期** | **状态**
--- | --- | ---
[Your Name] | [Date] | [Status: 计划中/进行中/完成/回滚]
