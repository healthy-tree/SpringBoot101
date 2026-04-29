package com.example.springboot.repository.jpa;

import com.example.springboot.model.Order;
import com.example.springboot.statemachine.OrderState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 订单 JPA Repository
 * 用于 MySQL 数据库的数据访问操作
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * 根据订单号查询订单
     */
    Optional<Order> findByOrderNumber(String orderNumber);

    /**
     * 根据客户 ID 查询所有订单
     */
    List<Order> findByCustomerId(String customerId);

    /**
     * 根据客户 ID 和状态分页查询订单
     */
    Page<Order> findByCustomerIdAndStatus(String customerId, OrderState status, Pageable pageable);

    /**
     * 根据状态查询所有订单
     */
    List<Order> findByStatus(OrderState status);

    /**
     * 根据状态分页查询订单
     */
    Page<Order> findByStatus(OrderState status, Pageable pageable);

    /**
     * 查询待支付的订单
     */
    @Query("SELECT o FROM Order o WHERE o.status = 'PENDING_PAYMENT' ORDER BY o.createdAt ASC")
    List<Order> findPendingPaymentOrders();

    /**
     * 查询指定时间范围内创建的订单
     */
    @Query("SELECT o FROM Order o WHERE o.createdAt BETWEEN :startTime AND :endTime ORDER BY o.createdAt DESC")
    List<Order> findOrdersByCreatedAtRange(
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    /**
     * 查询指定时间范围内创建的订单（分页）
     */
    @Query("SELECT o FROM Order o WHERE o.createdAt BETWEEN :startTime AND :endTime")
    Page<Order> findOrdersByCreatedAtRange(
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime,
        Pageable pageable
    );

    /**
     * 查询客户的订单总金额
     */
    @Query("SELECT SUM(o.amount) FROM Order o WHERE o.customerId = :customerId")
    java.math.BigDecimal getTotalAmountByCustomerId(@Param("customerId") String customerId);

    /**
     * 查询客户的已支付订单总金额
     */
    @Query("SELECT SUM(o.amount) FROM Order o WHERE o.customerId = :customerId AND o.status IN ('PAID', 'PREPARING', 'SHIPPED', 'DELIVERED')")
    java.math.BigDecimal getPaidTotalAmountByCustomerId(@Param("customerId") String customerId);

    /**
     * 查询客户的订单数量
     */
    long countByCustomerId(String customerId);

    /**
     * 查询客户的已完成订单数量
     */
    long countByCustomerIdAndStatus(String customerId, OrderState status);

    /**
     * 查询未支付的订单（超过指定时间）
     */
    @Query("SELECT o FROM Order o WHERE o.status = 'PENDING_PAYMENT' AND o.createdAt < :beforeTime")
    List<Order> findOverdueOrders(@Param("beforeTime") LocalDateTime beforeTime);

    /**
     * 查询已支付但未发货的订单
     */
    @Query("SELECT o FROM Order o WHERE o.status IN ('PAID', 'PREPARING') AND o.paidAt IS NOT NULL")
    List<Order> findPaidButNotShippedOrders();

    /**
     * 查询已发货的订单
     */
    List<Order> findByStatusIn(List<OrderState> statuses);

    /**
     * 根据订单号和客户 ID 查询订单
     */
    @Query("SELECT o FROM Order o WHERE o.orderNumber = :orderNumber AND o.customerId = :customerId")
    Optional<Order> findByOrderNumberAndCustomerId(
        @Param("orderNumber") String orderNumber,
        @Param("customerId") String customerId
    );
}
