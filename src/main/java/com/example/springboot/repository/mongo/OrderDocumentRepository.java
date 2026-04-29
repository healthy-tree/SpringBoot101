package com.example.springboot.repository.mongo;

import com.example.springboot.model.OrderDocument;
import com.example.springboot.statemachine.OrderState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 订单 MongoDB Repository
 * 用于 MongoDB 数据库的数据访问操作
 * 与 JPA OrderRepository 保持接口一致，便于迁移
 */
@Repository
public interface OrderDocumentRepository extends MongoRepository<OrderDocument, String> {

    /**
     * 根据订单号查询订单
     */
    Optional<OrderDocument> findByOrderNumber(String orderNumber);

    /**
     * 根据客户 ID 查询所有订单
     */
    List<OrderDocument> findByCustomerId(String customerId);

    /**
     * 根据客户 ID 和状态分页查询订单
     */
    Page<OrderDocument> findByCustomerIdAndStatus(String customerId, OrderState status, Pageable pageable);

    /**
     * 根据状态查询所有订单
     */
    List<OrderDocument> findByStatus(OrderState status);

    /**
     * 根据状态分页查询订单
     */
    Page<OrderDocument> findByStatus(OrderState status, Pageable pageable);

    /**
     * 查询待支付的订单
     */
    @Query("{ 'status': 'PENDING_PAYMENT' }")
    List<OrderDocument> findPendingPaymentOrders();

    /**
     * 查询指定时间范围内创建的订单
     */
    @Query("{ 'createdAt': { '$gte': ?0, '$lte': ?1 } }")
    List<OrderDocument> findOrdersByCreatedAtRange(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 查询指定时间范围内创建的订单（分页）
     */
    Page<OrderDocument> findByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * 查询客户的订单总数
     */
    long countByCustomerId(String customerId);

    /**
     * 查询客户的指定状态订单总数
     */
    long countByCustomerIdAndStatus(String customerId, OrderState status);

    /**
     * 查询未支付的订单（超过指定时间）
     */
    @Query("{ 'status': 'PENDING_PAYMENT', 'createdAt': { '$lt': ?0 } }")
    List<OrderDocument> findOverdueOrders(LocalDateTime beforeTime);

    /**
     * 查询已支付但未发货的订单
     */
    @Query("{ 'status': { '$in': ['PAID', 'PREPARING'] }, 'paidAt': { '$ne': null } }")
    List<OrderDocument> findPaidButNotShippedOrders();

    /**
     * 根据状态列表查询订单
     */
    List<OrderDocument> findByStatusIn(List<OrderState> statuses);

    /**
     * 根据订单号和客户 ID 查询订单
     */
    Optional<OrderDocument> findByOrderNumberAndCustomerId(String orderNumber, String customerId);

    /**
     * 查询待迁移的订单（标记为 MONGODB 的订单）
     */
    @Query("{ 'dataSource': 'MONGODB', 'migratedAt': null }")
    List<OrderDocument> findPendingMigrationOrders();

    /**
     * 查询已迁移的订单
     */
    @Query("{ 'dataSource': 'MIGRATED', 'migratedAt': { '$ne': null } }")
    List<OrderDocument> findMigratedOrders();

    /**
     * 按客户 ID 分页查询订单
     */
    Page<OrderDocument> findByCustomerId(String customerId, Pageable pageable);

    /**
     * 查询最近修改的订单
     */
    List<OrderDocument> findTop10ByOrderByUpdatedAtDesc();

    /**
     * 按创建时间倒序查询订单（分页）
     */
    Page<OrderDocument> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * 删除已取消且超过指定时间的订单
     */
    @Query(value = "{ 'status': 'CANCELLED', 'cancelledAt': { '$lt': ?0 } }", delete = true)
    long deleteCancelledOrdersBefore(LocalDateTime beforeTime);
}
