package com.example.springboot.statemachine;

/**
 * 订单事件枚举
 * 用于定义触发订单状态转换的各种事件
 */
public enum OrderEvent {
    /**
     * 支付事件
     */
    PAY,

    /**
     * 开始准备事件
     */
    PREPARE,

    /**
     * 发货事件
     */
    SHIP,

    /**
     * 签收事件
     */
    DELIVER,

    /**
     * 取消事件
     */
    CANCEL,

    /**
     * 退款事件
     */
    REFUND,

    /**
     * 异常恢复事件
     */
    RECOVER
}
