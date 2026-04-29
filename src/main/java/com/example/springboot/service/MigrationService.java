package com.example.springboot.service;

import com.example.springboot.model.Order;
import com.example.springboot.model.OrderDocument;
import com.example.springboot.repository.jpa.OrderRepository;
import com.example.springboot.repository.mongo.OrderDocumentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 迁移服务类
 * 处理从MongoDB到MySQL的数据迁移
 *
 * 功能特性：
 * - 支持全量迁移和增量迁移
 * - 数据验证和冲突检测
 * - 迁移进度追踪
 * - 支持回滚操作
 * - 性能优化（批量处理）
 */
@Slf4j
@Service
public class MigrationService {

    @Autowired(required = false)
    private OrderDocumentRepository orderDocumentRepository;

    @Autowired(required = false)
    private OrderRepository orderRepository;

    private static final int BATCH_SIZE = 100;

    /**
     * 迁移结果统计类
     */
    public static class MigrationResult {
        public int totalRecords;
        public int successCount;
        public int failureCount;
        public int skipCount;
        public LocalDateTime startTime;
        public LocalDateTime endTime;
        public List<String> errors;

        public MigrationResult() {
            this.errors = new ArrayList<>();
            this.startTime = LocalDateTime.now();
        }

        public void addError(String error) {
            this.errors.add(error);
            this.failureCount++;
        }

        public long getDurationSeconds() {
            if (endTime == null) return 0;
            return java.time.temporal.ChronoUnit.SECONDS.between(startTime, endTime);
        }

        @Override
        public String toString() {
            return String.format(
                "MigrationResult{total=%d, success=%d, failure=%d, skip=%d, duration=%ds}",
                totalRecords, successCount, failureCount, skipCount, getDurationSeconds()
            );
        }
    }

    /**
     * 全量迁移：将MongoDB中的所有订单迁移到MySQL
     */
    @Transactional
    public MigrationResult migrateAll() {
        log.info("开始全量迁移：MongoDB -> MySQL");
        MigrationResult result = new MigrationResult();

        try {
            if (orderDocumentRepository == null || orderRepository == null) {
                log.error("数据源配置不正确，无法进行迁移");
                result.addError("数据源未正确配置");
                result.endTime = LocalDateTime.now();
                return result;
            }

            // 获取MongoDB中的所有订单
            List<OrderDocument> allDocuments = orderDocumentRepository.findAll();
            result.totalRecords = allDocuments.size();

            log.info("找到 {} 条待迁移记录", result.totalRecords);

            // 批量迁移
            for (int i = 0; i < allDocuments.size(); i += BATCH_SIZE) {
                int end = Math.min(i + BATCH_SIZE, allDocuments.size());
                List<OrderDocument> batch = allDocuments.subList(i, end);

                for (OrderDocument document : batch) {
                    try {
                        migrateDocument(document, result);
                    } catch (Exception e) {
                        log.error("迁移订单 {} 失败", document.getOrderNumber(), e);
                        result.addError("迁移订单失败: " + document.getOrderNumber() + " - " + e.getMessage());
                    }
                }

                log.info("已处理 {}/{} 条记录", Math.min(i + BATCH_SIZE, allDocuments.size()), result.totalRecords);
            }

            result.endTime = LocalDateTime.now();
            log.info("全量迁移完成: {}", result);

        } catch (Exception e) {
            log.error("全量迁移异常", e);
            result.addError("全量迁移异常: " + e.getMessage());
            result.endTime = LocalDateTime.now();
        }

        return result;
    }

    /**
     * 增量迁移：仅迁移未迁移的订单
     */
    @Transactional
    public MigrationResult migrateIncremental() {
        log.info("开始增量迁移：MongoDB -> MySQL");
        MigrationResult result = new MigrationResult();

        try {
            if (orderDocumentRepository == null || orderRepository == null) {
                log.error("数据源配置不正确，无法进行迁移");
                result.addError("数据源未正确配置");
                result.endTime = LocalDateTime.now();
                return result;
            }

            // 获取待迁移的订单（标记为MONGODB且未迁移的）
            List<OrderDocument> pendingDocuments = orderDocumentRepository.findPendingMigrationOrders();
            result.totalRecords = pendingDocuments.size();

            log.info("找到 {} 条待迁移记录", result.totalRecords);

            // 批量迁移
            for (int i = 0; i < pendingDocuments.size(); i += BATCH_SIZE) {
                int end = Math.min(i + BATCH_SIZE, pendingDocuments.size());
                List<OrderDocument> batch = pendingDocuments.subList(i, end);

                for (OrderDocument document : batch) {
                    try {
                        migrateDocument(document, result);
                    } catch (Exception e) {
                        log.error("迁移订单 {} 失败", document.getOrderNumber(), e);
                        result.addError("迁移订单失败: " + document.getOrderNumber() + " - " + e.getMessage());
                    }
                }

                log.info("已处理 {}/{} 条记录", Math.min(i + BATCH_SIZE, pendingDocuments.size()), result.totalRecords);
            }

            result.endTime = LocalDateTime.now();
            log.info("增量迁移完成: {}", result);

        } catch (Exception e) {
            log.error("增量迁移异常", e);
            result.addError("增量迁移异常: " + e.getMessage());
            result.endTime = LocalDateTime.now();
        }

        return result;
    }

    /**
     * 迁移单个文档
     */
    private void migrateDocument(OrderDocument document, MigrationResult result) {
        // 检查是否已迁移
        Optional<Order> existing = orderRepository.findByOrderNumber(document.getOrderNumber());
        if (existing.isPresent()) {
            log.warn("订单 {} 已存在于MySQL中，跳过迁移", document.getOrderNumber());
            result.skipCount++;
            return;
        }

        // 转换Document到Order
        Order order = new Order();
        order.setOrderNumber(document.getOrderNumber());
        order.setCustomerId(document.getCustomerId());
        order.setCustomerName(document.getCustomerName());
        order.setAmount(document.getAmount());
        order.setDescription(document.getDescription());
        order.setStatus(document.getStatus());
        order.setRemarks(document.getRemarks());
        order.setCreatedAt(document.getCreatedAt() != null ? document.getCreatedAt() : LocalDateTime.now());
        order.setUpdatedAt(document.getUpdatedAt() != null ? document.getUpdatedAt() : LocalDateTime.now());
        order.setPaidAt(document.getPaidAt());
        order.setShippedAt(document.getShippedAt());
        order.setDeliveredAt(document.getDeliveredAt());
        order.setCancelledAt(document.getCancelledAt());

        // 保存到MySQL
        try {
            orderRepository.save(order);
            result.successCount++;

            // 更新MongoDB文档的迁移状态
            document.setDataSource("MIGRATED");
            document.setMigratedAt(LocalDateTime.now());
            orderDocumentRepository.save(document);

            log.debug("订单 {} 迁移成功", document.getOrderNumber());

        } catch (Exception e) {
            log.error("保存订单 {} 失败", document.getOrderNumber(), e);
            throw new RuntimeException("保存订单失败: " + e.getMessage());
        }
    }

    /**
     * 迁移指定客户的所有订单
     */
    @Transactional
    public MigrationResult migrateByCustomerId(String customerId) {
        log.info("开始迁移客户 {} 的所有订单", customerId);
        MigrationResult result = new MigrationResult();

        try {
            if (orderDocumentRepository == null || orderRepository == null) {
                result.addError("数据源未正确配置");
                result.endTime = LocalDateTime.now();
                return result;
            }

            // 获取该客户的所有订单
            List<OrderDocument> customerDocuments = orderDocumentRepository.findByCustomerId(customerId);
            result.totalRecords = customerDocuments.size();

            log.info("找到客户 {} 的 {} 条订单", customerId, result.totalRecords);

            for (OrderDocument document : customerDocuments) {
                try {
                    migrateDocument(document, result);
                } catch (Exception e) {
                    log.error("迁移订单 {} 失败", document.getOrderNumber(), e);
                    result.addError("迁移订单失败: " + document.getOrderNumber());
                }
            }

            result.endTime = LocalDateTime.now();
            log.info("客户迁移完成: {}", result);

        } catch (Exception e) {
            log.error("客户迁移异常", e);
            result.addError("客户迁移异常: " + e.getMessage());
            result.endTime = LocalDateTime.now();
        }

        return result;
    }

    /**
     * 数据验证：检查迁移是否完整
     */
    public MigrationResult validateMigration() {
        log.info("开始数据验证");
        MigrationResult result = new MigrationResult();

        try {
            if (orderDocumentRepository == null || orderRepository == null) {
                result.addError("数据源未正确配置");
                result.endTime = LocalDateTime.now();
                return result;
            }

            // 获取所有MongoDB文档
            List<OrderDocument> allDocuments = orderDocumentRepository.findAll();
            result.totalRecords = allDocuments.size();

            // 检查每个文档是否在MySQL中存在
            for (OrderDocument document : allDocuments) {
                Optional<Order> mysqlOrder = orderRepository.findByOrderNumber(document.getOrderNumber());

                if (mysqlOrder.isEmpty()) {
                    result.addError("订单 " + document.getOrderNumber() + " 在MySQL中不存在");
                } else {
                    // 验证关键字段是否一致
                    Order order = mysqlOrder.get();
                    if (!validateOrderMatch(document, order)) {
                        result.addError("订单 " + document.getOrderNumber() + " 数据不一致");
                    } else {
                        result.successCount++;
                    }
                }
            }

            result.endTime = LocalDateTime.now();
            log.info("数据验证完成: {}", result);

        } catch (Exception e) {
            log.error("数据验证异常", e);
            result.addError("数据验证异常: " + e.getMessage());
            result.endTime = LocalDateTime.now();
        }

        return result;
    }

    /**
     * 验证Document和Order的数据是否匹配
     */
    private boolean validateOrderMatch(OrderDocument document, Order order) {
        return document.getOrderNumber().equals(order.getOrderNumber()) &&
               document.getCustomerId().equals(order.getCustomerId()) &&
               document.getAmount().compareTo(order.getAmount()) == 0 &&
               document.getStatus() == order.getStatus();
    }

    /**
     * 获取迁移进度
     */
    public MigrationProgress getMigrationProgress() {
        MigrationProgress progress = new MigrationProgress();

        try {
            if (orderDocumentRepository == null) {
                return progress;
            }

            // 统计MongoDB中的总订单数
            long totalInMongo = orderDocumentRepository.count();

            // 统计已迁移的订单数
            List<OrderDocument> migratedDocuments = orderDocumentRepository.findMigratedOrders();
            long migratedCount = migratedDocuments.size();

            // 统计待迁移的订单数
            List<OrderDocument> pendingDocuments = orderDocumentRepository.findPendingMigrationOrders();
            long pendingCount = pendingDocuments.size();

            progress.totalRecords = totalInMongo;
            progress.migratedRecords = migratedCount;
            progress.pendingRecords = pendingCount;
            progress.progressPercentage = totalInMongo > 0 ? (migratedCount * 100 / totalInMongo) : 0;

        } catch (Exception e) {
            log.error("获取迁移进度失败", e);
        }

        return progress;
    }

    /**
     * 迁移进度统计类
     */
    public static class MigrationProgress {
        public long totalRecords;
        public long migratedRecords;
        public long pendingRecords;
        public long progressPercentage;

        @Override
        public String toString() {
            return String.format(
                "MigrationProgress{total=%d, migrated=%d, pending=%d, progress=%d%%}",
                totalRecords, migratedRecords, pendingRecords, progressPercentage
            );
        }
    }

    /**
     * 回滚迁移：删除MySQL中的迁移数据
     * 仅删除标记为 dataSource='MIGRATED' 的订单
     */
    @Transactional
    public MigrationResult rollbackMigration() {
        log.warn("开始回滚迁移操作");
        MigrationResult result = new MigrationResult();

        try {
            if (orderDocumentRepository == null || orderRepository == null) {
                result.addError("数据源未正确配置");
                result.endTime = LocalDateTime.now();
                return result;
            }

            // 获取所有已迁移的订单
            List<OrderDocument> migratedDocuments = orderDocumentRepository.findMigratedOrders();
            result.totalRecords = migratedDocuments.size();

            log.info("找到 {} 条已迁移的订单，准备回滚", result.totalRecords);

            for (OrderDocument document : migratedDocuments) {
                try {
                    Optional<Order> order = orderRepository.findByOrderNumber(document.getOrderNumber());
                    if (order.isPresent()) {
                        orderRepository.deleteById(order.get().getId());
                        result.successCount++;

                        // 重置MongoDB文档的迁移标记
                        document.setDataSource("MONGODB");
                        document.setMigratedAt(null);
                        orderDocumentRepository.save(document);

                        log.debug("订单 {} 回滚成功", document.getOrderNumber());
                    }
                } catch (Exception e) {
                    log.error("回滚订单 {} 失败", document.getOrderNumber(), e);
                    result.addError("回滚订单失败: " + document.getOrderNumber());
                }
            }

            result.endTime = LocalDateTime.now();
            log.info("迁移回滚完成: {}", result);

        } catch (Exception e) {
            log.error("迁移回滚异常", e);
            result.addError("迁移回滚异常: " + e.getMessage());
            result.endTime = LocalDateTime.now();
        }

        return result;
    }
}
