package com.example.springboot.controller;

import com.example.springboot.model.Order;
import com.example.springboot.service.MigrationService;
import com.example.springboot.service.OrderService;
import com.example.springboot.statemachine.OrderState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 订单 REST API 控制器
 * 提供订单相关的 RESTful 接口
 *
 * 基础路径: /api/orders
 */
@Slf4j
@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired(required = false)
    private MigrationService migrationService;

    /**
     * 创建订单
     * POST /api/orders
     *
     * 请求体：
     * {
     *   "customerId": "CUST001",
     *   "customerName": "张三",
     *   "amount": 99.99,
     *   "description": "购买商品描述"
     * }
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createOrder(
            @RequestParam String customerId,
            @RequestParam String customerName,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) String description) {

        log.info("Creating order for customer: {}", customerId);

        try {
            Order order = orderService.createOrder(customerId, customerName, amount, description);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "订单创建成功");
            response.put("data", order);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Failed to create order", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "订单创建失败: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 根据订单 ID 获取订单
     * GET /api/orders/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getOrderById(@PathVariable Long id) {
        log.info("Getting order by ID: {}", id);

        Optional<Order> order = orderService.getOrderById(id);

        if (order.isPresent()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", order.get());
            return ResponseEntity.ok(response);
        } else {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "订单不存在");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    /**
     * 根据订单号获取订单
     * GET /api/orders/number/{orderNumber}
     */
    @GetMapping("/number/{orderNumber}")
    public ResponseEntity<Map<String, Object>> getOrderByNumber(@PathVariable String orderNumber) {
        log.info("Getting order by number: {}", orderNumber);

        Optional<Order> order = orderService.getOrderByNumber(orderNumber);

        if (order.isPresent()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", order.get());
            return ResponseEntity.ok(response);
        } else {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "订单不存在");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    /**
     * 根据客户 ID 获取所有订单
     * GET /api/orders/customer/{customerId}
     */
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<Map<String, Object>> getOrdersByCustomerId(@PathVariable String customerId) {
        log.info("Getting orders for customer: {}", customerId);

        List<Order> orders = orderService.getOrdersByCustomerId(customerId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", orders);
        response.put("count", orders.size());

        return ResponseEntity.ok(response);
    }

    /**
     * 根据订单状态获取订单
     * GET /api/orders/status/{status}
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<Map<String, Object>> getOrdersByStatus(@PathVariable String status) {
        log.info("Getting orders with status: {}", status);

        try {
            OrderState orderStatus = OrderState.valueOf(status.toUpperCase());
            List<Order> orders = orderService.getOrdersByStatus(orderStatus);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", orders);
            response.put("count", orders.size());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "无效的订单状态: " + status);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 支付订单
     * POST /api/orders/{orderNumber}/pay
     */
    @PostMapping("/{orderNumber}/pay")
    public ResponseEntity<Map<String, Object>> payOrder(@PathVariable String orderNumber) {
        log.info("Processing payment for order: {}", orderNumber);

        try {
            Order order = orderService.payOrder(orderNumber);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "订单支付成功");
            response.put("data", order);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to pay order: {}", orderNumber, e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "订单支付失败: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 准备订单
     * POST /api/orders/{orderNumber}/prepare
     */
    @PostMapping("/{orderNumber}/prepare")
    public ResponseEntity<Map<String, Object>> prepareOrder(@PathVariable String orderNumber) {
        log.info("Preparing order: {}", orderNumber);

        try {
            Order order = orderService.prepareOrder(orderNumber);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "订单准备成功");
            response.put("data", order);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to prepare order: {}", orderNumber, e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "订单准备失败: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 发货订单
     * POST /api/orders/{orderNumber}/ship
     */
    @PostMapping("/{orderNumber}/ship")
    public ResponseEntity<Map<String, Object>> shipOrder(@PathVariable String orderNumber) {
        log.info("Shipping order: {}", orderNumber);

        try {
            Order order = orderService.shipOrder(orderNumber);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "订单发货成功");
            response.put("data", order);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to ship order: {}", orderNumber, e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "订单发货失败: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 签收订单
     * POST /api/orders/{orderNumber}/deliver
     */
    @PostMapping("/{orderNumber}/deliver")
    public ResponseEntity<Map<String, Object>> deliverOrder(@PathVariable String orderNumber) {
        log.info("Delivering order: {}", orderNumber);

        try {
            Order order = orderService.deliverOrder(orderNumber);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "订单签收成功");
            response.put("data", order);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to deliver order: {}", orderNumber, e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "订单签收失败: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 取消订单
     * POST /api/orders/{orderNumber}/cancel
     */
    @PostMapping("/{orderNumber}/cancel")
    public ResponseEntity<Map<String, Object>> cancelOrder(
            @PathVariable String orderNumber,
            @RequestParam(required = false) String reason) {
        log.info("Cancelling order: {}", orderNumber);

        try {
            Order order = orderService.cancelOrder(orderNumber, reason);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "订单取消成功");
            response.put("data", order);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to cancel order: {}", orderNumber, e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "订单取消失败: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 退款订单
     * POST /api/orders/{orderNumber}/refund
     */
    @PostMapping("/{orderNumber}/refund")
    public ResponseEntity<Map<String, Object>> refundOrder(@PathVariable String orderNumber) {
        log.info("Refunding order: {}", orderNumber);

        try {
            Order order = orderService.refundOrder(orderNumber);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "订单退款成功");
            response.put("data", order);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to refund order: {}", orderNumber, e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "订单退款失败: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 获取客户的订单总金额
     * GET /api/orders/customer/{customerId}/total-amount
     */
    @GetMapping("/customer/{customerId}/total-amount")
    public ResponseEntity<Map<String, Object>> getTotalAmount(@PathVariable String customerId) {
        log.info("Getting total amount for customer: {}", customerId);

        BigDecimal totalAmount = orderService.getTotalAmountByCustomerId(customerId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("customerId", customerId);
        response.put("totalAmount", totalAmount);

        return ResponseEntity.ok(response);
    }

    /**
     * 获取客户的已支付订单总金额
     * GET /api/orders/customer/{customerId}/paid-amount
     */
    @GetMapping("/customer/{customerId}/paid-amount")
    public ResponseEntity<Map<String, Object>> getPaidAmount(@PathVariable String customerId) {
        log.info("Getting paid amount for customer: {}", customerId);

        BigDecimal paidAmount = orderService.getPaidTotalAmountByCustomerId(customerId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("customerId", customerId);
        response.put("paidAmount", paidAmount);

        return ResponseEntity.ok(response);
    }

    /**
     * 获取客户的订单数量
     * GET /api/orders/customer/{customerId}/count
     */
    @GetMapping("/customer/{customerId}/count")
    public ResponseEntity<Map<String, Object>> getOrderCount(@PathVariable String customerId) {
        log.info("Getting order count for customer: {}", customerId);

        long count = orderService.getOrderCountByCustomerId(customerId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("customerId", customerId);
        response.put("orderCount", count);

        return ResponseEntity.ok(response);
    }

    /**
     * 全量迁移：MongoDB -> MySQL
     * POST /api/orders/migration/migrate-all
     */
    @PostMapping("/migration/migrate-all")
    public ResponseEntity<Map<String, Object>> migrateAll() {
        log.info("Starting full migration: MongoDB -> MySQL");

        if (migrationService == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "迁移服务未启用");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
        }

        try {
            MigrationService.MigrationResult result = migrationService.migrateAll();

            Map<String, Object> response = new HashMap<>();
            response.put("success", result.failureCount == 0);
            response.put("message", "全量迁移完成");
            response.put("data", result);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Migration failed", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "迁移失败: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 增量迁移：MongoDB -> MySQL
     * POST /api/orders/migration/migrate-incremental
     */
    @PostMapping("/migration/migrate-incremental")
    public ResponseEntity<Map<String, Object>> migrateIncremental() {
        log.info("Starting incremental migration: MongoDB -> MySQL");

        if (migrationService == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "迁移服务未启用");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
        }

        try {
            MigrationService.MigrationResult result = migrationService.migrateIncremental();

            Map<String, Object> response = new HashMap<>();
            response.put("success", result.failureCount == 0);
            response.put("message", "增量迁移完成");
            response.put("data", result);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Incremental migration failed", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "增量迁移失败: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 按客户迁移：迁移指定客户的所有订单
     * POST /api/orders/migration/migrate-by-customer/{customerId}
     */
    @PostMapping("/migration/migrate-by-customer/{customerId}")
    public ResponseEntity<Map<String, Object>> migrateByCustomerId(@PathVariable String customerId) {
        log.info("Starting migration for customer: {}", customerId);

        if (migrationService == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "迁移服务未启用");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
        }

        try {
            MigrationService.MigrationResult result = migrationService.migrateByCustomerId(customerId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", result.failureCount == 0);
            response.put("message", "客户迁移完成");
            response.put("data", result);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Customer migration failed", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "客户迁移失败: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 验证迁移数据
     * GET /api/orders/migration/validate
     */
    @GetMapping("/migration/validate")
    public ResponseEntity<Map<String, Object>> validateMigration() {
        log.info("Starting migration validation");

        if (migrationService == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "迁移服务未启用");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
        }

        try {
            MigrationService.MigrationResult result = migrationService.validateMigration();

            Map<String, Object> response = new HashMap<>();
            response.put("success", result.failureCount == 0);
            response.put("message", "迁移验证完成");
            response.put("data", result);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Migration validation failed", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "迁移验证失败: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 获取迁移进度
     * GET /api/orders/migration/progress
     */
    @GetMapping("/migration/progress")
    public ResponseEntity<Map<String, Object>> getMigrationProgress() {
        log.info("Getting migration progress");

        if (migrationService == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "迁移服务未启用");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
        }

        try {
            MigrationService.MigrationProgress progress = migrationService.getMigrationProgress();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", progress);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get migration progress", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "获取迁移进度失败: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 回滚迁移
     * POST /api/orders/migration/rollback
     */
    @PostMapping("/migration/rollback")
    public ResponseEntity<Map<String, Object>> rollbackMigration() {
        log.warn("Rolling back migration");

        if (migrationService == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "迁移服务未启用");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
        }

        try {
            MigrationService.MigrationResult result = migrationService.rollbackMigration();

            Map<String, Object> response = new HashMap<>();
            response.put("success", result.failureCount == 0);
            response.put("message", "迁移回滚完成");
            response.put("data", result);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Migration rollback failed", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "迁移回滚失败: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 健康检查
     * GET /api/orders/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("status", "UP");
        response.put("message", "Order API is running");
        return ResponseEntity.ok(response);
    }
}
