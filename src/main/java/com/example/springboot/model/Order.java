package com.example.springboot.model;

import com.example.springboot.statemachine.OrderState;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单 JPA 实体类
 * 用于关系型数据库的持久化存储
 * 支持 MySQL、H2 等关系型数据库
 */
@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_order_number", columnList = "order_number", unique = true),
    @Index(name = "idx_customer_id", columnList = "customer_id"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    /**
     * 订单 ID（主键）
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 订单号（唯一）
     */
    @Column(name = "order_number", nullable = false, unique = true, length = 32)
    private String orderNumber;

    /**
     * 客户 ID
     */
    @Column(name = "customer_id", nullable = false)
    private String customerId;

    /**
     * 客户名称
     */
    @Column(name = "customer_name", length = 100)
    private String customerName;

    /**
     * 订单金额
     */
    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    /**
     * 订单描述
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * 订单状态
     */
    @Column(name = "status", nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private OrderState status;

    /**
     * 备注信息
     */
    @Column(name = "remarks", length = 500)
    private String remarks;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 支付时间
     */
    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    /**
     * 发货时间
     */
    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    /**
     * 签收时间
     */
    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    /**
     * 取消时间
     */
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    /**
     * 版本号（用于乐观锁）
     */
    @Version
    @Column(name = "version")
    private Long version;

    /**
     * JPA 生命周期回调 - 在持久化前调用
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = OrderState.PENDING_PAYMENT;
        }
    }

    /**
     * JPA 生命周期回调 - 在更新前调用
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
