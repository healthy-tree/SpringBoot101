package com.example.springboot.statemachine;

/**
 * 订单状态枚举
 * 用于定义订单在状态机中的各种状态
 */
public enum OrderState {
    /**
     * 待支付状态
     */
    PENDING_PAYMENT,

    /**
     * 已支付状态
     */
    PAID,

    /**
     * 准备中状态
     */
    PREPARING,

    /**
     * 已发货状态
     */
    SHIPPED,

    /**
     * 已签收状态
     */
    DELIVERED,

    /**
     * 已取消状态
     */
    CANCELLED,

    /**
     * 已退款状态
     */
    REFUNDED
}
