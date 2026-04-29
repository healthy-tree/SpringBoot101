package com.example.springboot.model;

import com.example.springboot.statemachine.OrderState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单 MongoDB 文档类
 * 用于 MongoDB 数据库的持久化存储
 * 与 Order JPA 实体对应，支持从 MongoDB 迁移到 MySQL
 */
@Document(collection = "orders")
@CompoundIndex(name = "idx_customer_status", def = "{'customerId': 1, 'status': 1}")
@CompoundIndex(name = "idx_created_status", def = "{'createdAt': -1, 'status': 1}")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDocument {

    /**
     * 订单 ID（MongoDB ObjectId）
     * 在 MongoDB 中作为主键，迁移时可转换为 MySQL 的 UUID 或自增 ID
     */
    @Id
    private String id;

    /**
     * 订单号（唯一）
     * 业务层的唯一标识，跨数据库保持一致
     */
    @Indexed(unique = true)
    private String orderNumber;

    /**
     * 客户 ID
     * 用于查询和关联
     */
    @Indexed
    private String customerId;

    /**
     * 客户名称
     */
    private String customerName;

    /**
     * 订单金额
     */
    private BigDecimal amount;

    /**
     * 订单描述
     */
    private String description;

    /**
     * 订单状态
     * 使用枚举类型，MongoDB 以字符串形式存储
     */
    @Indexed
    private OrderState status;

    /**
     * 备注信息
     */
    private String remarks;

    /**
     * 创建时间
     */
    @Indexed
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 支付时间
     */
    private LocalDateTime paidAt;

    /**
     * 发货时间
     */
    private LocalDateTime shippedAt;

    /**
     * 签收时间
     */
    private LocalDateTime deliveredAt;

    /**
     * 取消时间
     */
    private LocalDateTime cancelledAt;

    /**
     * 数据来源标记
     * 用于迁移过程中追踪数据源
     * 值：MONGODB, MYSQL, MIGRATED
     */
    private String dataSource;

    /**
     * 迁移时间戳
     * 记录数据迁移完成时间
     */
    private LocalDateTime migratedAt;

    /**
     * 检查订单是否已支付
     */
    public boolean isPaid() {
        return this.status == OrderState.PAID ||
               this.status == OrderState.PREPARING ||
               this.status == OrderState.SHIPPED ||
               this.status == OrderState.DELIVERED;
    }

    /**
     * 检查订单是否已完成
     */
    public boolean isCompleted() {
        return this.status == OrderState.DELIVERED;
    }

    /**
     * 检查订单是否可以取消
     */
    public boolean canBeCancelled() {
        return this.status == OrderState.PENDING_PAYMENT;
    }
}
