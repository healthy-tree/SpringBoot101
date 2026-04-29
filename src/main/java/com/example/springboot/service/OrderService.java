package com.example.springboot.service;

import com.example.springboot.model.Order;
import com.example.springboot.model.OrderDocument;
import com.example.springboot.repository.jpa.OrderRepository;
import com.example.springboot.repository.mongo.OrderDocumentRepository;
import com.example.springboot.statemachine.OrderEvent;
import com.example.springboot.statemachine.OrderState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.StateMachineFactory;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 订单服务类
 * 集成 Spring Statemachine，处理订单的业务逻辑和状态转换
 *
 * 支持 MySQL 和 MongoDB 两种数据源
 */
@Slf4j
@Service
public class OrderService {

    @Autowired(required = false)
    private OrderRepository orderRepository;

    @Autowired(required = false)
    private OrderDocumentRepository orderDocumentRepository;

    @Autowired(required = false)
    private StateMachineFactory<OrderState, OrderEvent> stateMachineFactory;

    /**
     * 创建订单
     */
    @Transactional
    public Order createOrder(String customerId, String customerName, BigDecimal amount, String description) {
        log.info("Creating order for customer: {} with amount: {}", customerId, amount);

        Order order = Order.builder()
            .orderNumber(generateOrderNumber())
            .customerId(customerId)
            .customerName(customerName)
            .amount(amount)
            .description(description)
            .status(OrderState.PENDING_PAYMENT)
            .build();

        if (orderRepository != null) {
            order = orderRepository.save(order);
            log.info("Order created with ID: {} and number: {}", order.getId(), order.getOrderNumber());
        }

        return order;
    }

    /**
     * 创建 MongoDB 订单文档
     */
    @Transactional
    public OrderDocument createOrderDocument(String customerId, String customerName, BigDecimal amount, String description) {
        log.info("Creating MongoDB order document for customer: {} with amount: {}", customerId, amount);

        OrderDocument orderDocument = OrderDocument.builder()
            .orderNumber(generateOrderNumber())
            .customerId(customerId)
            .customerName(customerName)
            .amount(amount)
            .description(description)
            .status(OrderState.PENDING_PAYMENT)
            .dataSource("MONGODB")
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        if (orderDocumentRepository != null) {
            orderDocument = orderDocumentRepository.save(orderDocument);
            log.info("MongoDB order document created with ID: {} and number: {}", orderDocument.getId(), orderDocument.getOrderNumber());
        }

        return orderDocument;
    }

    /**
     * 根据 ID 获取订单
     */
    public Optional<Order> getOrderById(Long id) {
        if (orderRepository != null) {
            return orderRepository.findById(id);
        }
        return Optional.empty();
    }

    /**
     * 根据订单号获取订单
     */
    public Optional<Order> getOrderByNumber(String orderNumber) {
        if (orderRepository != null) {
            return orderRepository.findByOrderNumber(orderNumber);
        }
        return Optional.empty();
    }

    /**
     * 根据订单号获取 MongoDB 订单文档
     */
    public Optional<OrderDocument> getOrderDocumentByNumber(String orderNumber) {
        if (orderDocumentRepository != null) {
            return orderDocumentRepository.findByOrderNumber(orderNumber);
        }
        return Optional.empty();
    }

    /**
     * 根据客户 ID 获取所有订单
     */
    public List<Order> getOrdersByCustomerId(String customerId) {
        if (orderRepository != null) {
            return orderRepository.findByCustomerId(customerId);
        }
        return List.of();
    }

    /**
     * 分页获取客户订单
     */
    public Page<Order> getOrdersByCustomerId(String customerId, Pageable pageable) {
        if (orderRepository != null) {
            return orderRepository.findByCustomerIdAndStatus(customerId, null, pageable);
        }
        return Page.empty();
    }

    /**
     * 获取指定状态的订单
     */
    public List<Order> getOrdersByStatus(OrderState status) {
        if (orderRepository != null) {
            return orderRepository.findByStatus(status);
        }
        return List.of();
    }

    /**
     * 分页获取指定状态的订单
     */
    public Page<Order> getOrdersByStatus(OrderState status, Pageable pageable) {
        if (orderRepository != null) {
            return orderRepository.findByStatus(status, pageable);
        }
        return Page.empty();
    }

    /**
     * 处理订单支付
     * 触发 PAY 事件，将订单状态从 PENDING_PAYMENT 转换为 PAID
     */
    @Transactional
    public Order payOrder(String orderNumber) {
        log.info("Processing payment for order: {}", orderNumber);

        Optional<Order> orderOptional = getOrderByNumber(orderNumber);
        if (orderOptional.isEmpty()) {
            log.error("Order not found: {}", orderNumber);
            throw new RuntimeException("Order not found: " + orderNumber);
        }

        Order order = orderOptional.get();

        if (order.getStatus() != OrderState.PENDING_PAYMENT) {
            log.error("Order cannot be paid. Current status: {}", order.getStatus());
            throw new RuntimeException("Order cannot be paid in current state: " + order.getStatus());
        }

        // 使用状态机处理状态转换
        if (stateMachineFactory != null) {
            StateMachine<OrderState, OrderEvent> stateMachine = stateMachineFactory.getStateMachine();
            stateMachine.start();
            stateMachine.sendEvent(OrderEvent.PAY);
            // 获取新状态
            OrderState newState = stateMachine.getState().getId();
            order.setStatus(newState);
        } else {
            order.setStatus(OrderState.PAID);
        }

        order.setPaidAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        if (orderRepository != null) {
            order = orderRepository.save(order);
            log.info("Order paid successfully: {}", orderNumber);
        }

        return order;
    }

    /**
     * 处理订单准备
     * 触发 PREPARE 事件，将订单状态从 PAID 转换为 PREPARING
     */
    @Transactional
    public Order prepareOrder(String orderNumber) {
        log.info("Preparing order: {}", orderNumber);

        Optional<Order> orderOptional = getOrderByNumber(orderNumber);
        if (orderOptional.isEmpty()) {
            throw new RuntimeException("Order not found: " + orderNumber);
        }

        Order order = orderOptional.get();

        if (order.getStatus() != OrderState.PAID) {
            throw new RuntimeException("Order cannot be prepared in current state: " + order.getStatus());
        }

        if (stateMachineFactory != null) {
            StateMachine<OrderState, OrderEvent> stateMachine = stateMachineFactory.getStateMachine();
            stateMachine.start();
            stateMachine.sendEvent(OrderEvent.PREPARE);
            order.setStatus(stateMachine.getState().getId());
        } else {
            order.setStatus(OrderState.PREPARING);
        }

        order.setUpdatedAt(LocalDateTime.now());

        if (orderRepository != null) {
            order = orderRepository.save(order);
            log.info("Order prepared successfully: {}", orderNumber);
        }

        return order;
    }

    /**
     * 处理订单发货
     * 触发 SHIP 事件，将订单状态从 PREPARING 转换为 SHIPPED
     */
    @Transactional
    public Order shipOrder(String orderNumber) {
        log.info("Shipping order: {}", orderNumber);

        Optional<Order> orderOptional = getOrderByNumber(orderNumber);
        if (orderOptional.isEmpty()) {
            throw new RuntimeException("Order not found: " + orderNumber);
        }

        Order order = orderOptional.get();

        if (order.getStatus() != OrderState.PREPARING) {
            throw new RuntimeException("Order cannot be shipped in current state: " + order.getStatus());
        }

        if (stateMachineFactory != null) {
            StateMachine<OrderState, OrderEvent> stateMachine = stateMachineFactory.getStateMachine();
            stateMachine.start();
            stateMachine.sendEvent(OrderEvent.SHIP);
            order.setStatus(stateMachine.getState().getId());
        } else {
            order.setStatus(OrderState.SHIPPED);
        }

        order.setShippedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        if (orderRepository != null) {
            order = orderRepository.save(order);
            log.info("Order shipped successfully: {}", orderNumber);
        }

        return order;
    }

    /**
     * 处理订单签收
     * 触发 DELIVER 事件，将订单状态从 SHIPPED 转换为 DELIVERED
     */
    @Transactional
    public Order deliverOrder(String orderNumber) {
        log.info("Delivering order: {}", orderNumber);

        Optional<Order> orderOptional = getOrderByNumber(orderNumber);
        if (orderOptional.isEmpty()) {
            throw new RuntimeException("Order not found: " + orderNumber);
        }

        Order order = orderOptional.get();

        if (order.getStatus() != OrderState.SHIPPED) {
            throw new RuntimeException("Order cannot be delivered in current state: " + order.getStatus());
        }

        if (stateMachineFactory != null) {
            StateMachine<OrderState, OrderEvent> stateMachine = stateMachineFactory.getStateMachine();
            stateMachine.start();
            stateMachine.sendEvent(OrderEvent.DELIVER);
            order.setStatus(stateMachine.getState().getId());
        } else {
            order.setStatus(OrderState.DELIVERED);
        }

        order.setDeliveredAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        if (orderRepository != null) {
            order = orderRepository.save(order);
            log.info("Order delivered successfully: {}", orderNumber);
        }

        return order;
    }

    /**
     * 取消订单
     * 触发 CANCEL 事件，将订单状态转换为 CANCELLED
     */
    @Transactional
    public Order cancelOrder(String orderNumber, String reason) {
        log.info("Cancelling order: {} with reason: {}", orderNumber, reason);

        Optional<Order> orderOptional = getOrderByNumber(orderNumber);
        if (orderOptional.isEmpty()) {
            throw new RuntimeException("Order not found: " + orderNumber);
        }

        Order order = orderOptional.get();

        // 只有待支付和已支付状态的订单才能取消
        if (order.getStatus() != OrderState.PENDING_PAYMENT &&
            order.getStatus() != OrderState.PAID &&
            order.getStatus() != OrderState.PREPARING) {
            throw new RuntimeException("Order cannot be cancelled in current state: " + order.getStatus());
        }

        if (stateMachineFactory != null) {
            StateMachine<OrderState, OrderEvent> stateMachine = stateMachineFactory.getStateMachine();
            stateMachine.start();
            stateMachine.sendEvent(OrderEvent.CANCEL);
            order.setStatus(stateMachine.getState().getId());
        } else {
            order.setStatus(OrderState.CANCELLED);
        }

        order.setCancelledAt(LocalDateTime.now());
        order.setRemarks(reason);
        order.setUpdatedAt(LocalDateTime.now());

        if (orderRepository != null) {
            order = orderRepository.save(order);
            log.info("Order cancelled successfully: {}", orderNumber);
        }

        return order;
    }

    /**
     * 退款订单
     * 触发 REFUND 事件，将订单状态转换为 REFUNDED
     */
    @Transactional
    public Order refundOrder(String orderNumber) {
        log.info("Refunding order: {}", orderNumber);

        Optional<Order> orderOptional = getOrderByNumber(orderNumber);
        if (orderOptional.isEmpty()) {
            throw new RuntimeException("Order not found: " + orderNumber);
        }

        Order order = orderOptional.get();

        if (order.getStatus() != OrderState.CANCELLED && order.getStatus() != OrderState.PAID) {
            throw new RuntimeException("Order cannot be refunded in current state: " + order.getStatus());
        }

        if (stateMachineFactory != null) {
            StateMachine<OrderState, OrderEvent> stateMachine = stateMachineFactory.getStateMachine();
            stateMachine.start();
            stateMachine.sendEvent(OrderEvent.REFUND);
            order.setStatus(stateMachine.getState().getId());
        } else {
            order.setStatus(OrderState.REFUNDED);
        }

        order.setUpdatedAt(LocalDateTime.now());

        if (orderRepository != null) {
            order = orderRepository.save(order);
            log.info("Order refunded successfully: {}", orderNumber);
        }

        return order;
    }

    /**
     * 生成唯一的订单号
     */
    private String generateOrderNumber() {
        return "ORD-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * 获取待支付的订单
     */
    public List<Order> getPendingPaymentOrders() {
        if (orderRepository != null) {
            return orderRepository.findPendingPaymentOrders();
        }
        return List.of();
    }

    /**
     * 获取客户的总订单金额
     */
    public BigDecimal getTotalAmountByCustomerId(String customerId) {
        if (orderRepository != null) {
            BigDecimal total = orderRepository.getTotalAmountByCustomerId(customerId);
            return total != null ? total : BigDecimal.ZERO;
        }
        return BigDecimal.ZERO;
    }

    /**
     * 获取客户的已支付订单总金额
     */
    public BigDecimal getPaidTotalAmountByCustomerId(String customerId) {
        if (orderRepository != null) {
            BigDecimal total = orderRepository.getPaidTotalAmountByCustomerId(customerId);
            return total != null ? total : BigDecimal.ZERO;
        }
        return BigDecimal.ZERO;
    }

    /**
     * 获取客户的订单数量
     */
    public long getOrderCountByCustomerId(String customerId) {
        if (orderRepository != null) {
            return orderRepository.countByCustomerId(customerId);
        }
        return 0;
    }
}
