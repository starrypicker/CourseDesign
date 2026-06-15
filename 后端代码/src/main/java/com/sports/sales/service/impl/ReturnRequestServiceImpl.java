package com.sports.sales.service.impl;

import com.sports.sales.common.PageResult;
import com.sports.sales.entity.Invoice;
import com.sports.sales.entity.Orders;
import com.sports.sales.entity.OrderItem;
import com.sports.sales.entity.ReturnRequest;
import com.sports.sales.mapper.InvoiceMapper;
import com.sports.sales.mapper.OrderItemMapper;
import com.sports.sales.mapper.OrderMapper;
import com.sports.sales.mapper.ProductMapper;
import com.sports.sales.mapper.ReturnRequestMapper;
import com.sports.sales.service.ReturnRequestService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class ReturnRequestServiceImpl implements ReturnRequestService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ReturnRequestServiceImpl.class);

    private final ReturnRequestMapper returnRequestMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;
    private final InvoiceMapper invoiceMapper;
    private final CacheManager cacheManager;
    private final RedissonClient redissonClient;

    public ReturnRequestServiceImpl(ReturnRequestMapper returnRequestMapper,
                                     OrderMapper orderMapper,
                                     OrderItemMapper orderItemMapper,
                                     ProductMapper productMapper,
                                     InvoiceMapper invoiceMapper,
                                     CacheManager cacheManager,
                                     RedissonClient redissonClient) {
        this.returnRequestMapper = returnRequestMapper;
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.productMapper = productMapper;
        this.invoiceMapper = invoiceMapper;
        this.cacheManager = cacheManager;
        this.redissonClient = redissonClient;
    }

    @Override
    public PageResult<ReturnRequest> list(String customerCode, Integer status, Integer type,
                                           int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        Long total = returnRequestMapper.selectCount(customerCode, status, type);
        List<ReturnRequest> rows = returnRequestMapper.selectList(customerCode, status, type, offset, pageSize);
        // 转换类型和状态为中文名称
        rows.forEach(r -> {
            r.setTypeName(r.getType() != null && r.getType() == 1 ? "换货" : "退货退款");
            r.setStatusName(switch (r.getStatus() != null ? r.getStatus() : 0) {
                case 1 -> "已通过";
                case 2 -> "已拒绝";
                case 3 -> "已完成";
                default -> "待审核";
            });
        });
        return new PageResult<>(total, rows, pageNum, pageSize);
    }

    @Override
    public ReturnRequest getById(Long id) {
        ReturnRequest r = returnRequestMapper.selectById(id);
        if (r != null) {
            r.setTypeName(r.getType() != null && r.getType() == 1 ? "换货" : "退货退款");
            r.setStatusName(switch (r.getStatus() != null ? r.getStatus() : 0) {
                case 1 -> "已通过";
                case 2 -> "已拒绝";
                case 3 -> "已完成";
                default -> "待审核";
            });
        }
        return r;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ReturnRequest record) {
        // 校验订单存在且属于当前用户
        Orders order = orderMapper.selectById(record.getOrderId());
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!record.getCustomerCode().equals(order.getCustomerCode())) {
            throw new RuntimeException("无权操作该订单");
        }
        // 已取消的订单不可申请退换货
        if (order.getOrderStatus() != null && order.getOrderStatus() == 3) {
            throw new RuntimeException("已取消的订单不支持退换货申请");
        }
        // 仅订单已完成(2) 且 已签收(shippingStatus==2) 的订单可申请退换货
        boolean isCompleted = order.getOrderStatus() != null && order.getOrderStatus() == 2;
        boolean isSigned = order.getShippingStatus() != null && order.getShippingStatus() == 2;
        if (!isCompleted || !isSigned) {
            throw new RuntimeException("该订单当前状态不支持退换货申请，需订单已完成且已签收");
        }
        // 检查是否已有进行中的申请
        if (returnRequestMapper.countActiveByOrderId(record.getOrderId()) > 0) {
            throw new RuntimeException("该订单已有一个进行中的退换货申请");
        }
        // 检查是否已完成过退换货处理（防止库存重复回滚）
        if (returnRequestMapper.countCompletedByOrderId(record.getOrderId()) > 0) {
            throw new RuntimeException("该订单已完成退换货处理，无法重复申请");
        }
        // 检查是否已申请发票——已作废(status=2)的发票不阻止退换货
        Invoice invoice = invoiceMapper.selectByOrderId(record.getOrderId());
        if (invoice != null && (invoice.getStatus() == null || invoice.getStatus() != 2)) {
            String invoiceInfo = invoice.getStatus() != null && invoice.getStatus() == 1
                    ? "该订单已开具发票（发票号：" + invoice.getInvoiceNo() + "），请先联系管理员作废发票后再申请退换货"
                    : "该订单已申请发票（待开票），请先取消发票申请后再申请退换货";
            throw new RuntimeException(invoiceInfo);
        }
        // 如果是退货退款，默认退款金额为订单金额
        if (record.getType() == null || record.getType() == 0) {
            record.setRefundAmount(order.getTotalAmount());
        }
        returnRequestMapper.insert(record);
        log.info("退换货申请已提交, id={}, orderId={}, type={}", record.getId(), record.getOrderId(), record.getType());
        return record.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean approve(Long id, String adminRemark, BigDecimal refundAmount, String handledBy) {
        ReturnRequest record = returnRequestMapper.selectById(id);
        if (record == null) {
            throw new RuntimeException("退换货申请不存在");
        }
        if (record.getStatus() != 0) {
            throw new RuntimeException("该申请当前状态不允许此操作");
        }
        returnRequestMapper.updateStatus(id, 1, adminRemark, handledBy, refundAmount);
        log.info("退换货申请已通过, id={}", id);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean reject(Long id, String adminRemark, String handledBy) {
        ReturnRequest record = returnRequestMapper.selectById(id);
        if (record == null) {
            throw new RuntimeException("退换货申请不存在");
        }
        if (record.getStatus() != 0) {
            throw new RuntimeException("该申请当前状态不允许此操作");
        }
        returnRequestMapper.updateStatus(id, 2, adminRemark, handledBy, null);
        log.info("退换货申请已拒绝, id={}", id);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean complete(Long id) {
        ReturnRequest record = returnRequestMapper.selectById(id);
        if (record == null) {
            throw new RuntimeException("退换货申请不存在");
        }
        if (record.getStatus() != 1) {
            throw new RuntimeException("该申请当前状态不允许此操作");
        }

        // 分布式锁防止并发完成同一订单的退换货
        String lockKey = "lock:return:complete:" + record.getOrderId();
        RLock lock = redissonClient.getLock(lockKey);
        boolean syncRegistered = false;
        try {
            boolean locked = lock.tryLock(10, 30, TimeUnit.SECONDS);
            if (!locked) {
                throw new RuntimeException("系统繁忙，请稍后重试");
            }
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (lock.isHeldByCurrentThread()) {
                        try { lock.unlock(); } catch (Exception ignored) {}
                    }
                }
            });
            syncRegistered = true;

            if (record.getType() != null && record.getType() == 1) {
                // 换货：不涉及退款，标记完成并记录日志
                // 实际业务中换货需要创建新的换货出库单，此处标记原订单待管理员处理
                log.info("换货申请完成, id={}, orderId={}, 请管理员安排换货出库", id, record.getOrderId());
            } else {
                // 退货退款：回滚库存，更新订单支付状态为已退款
                Orders order = orderMapper.selectById(record.getOrderId());
                if (order == null) {
                    throw new RuntimeException("关联订单不存在");
                }
                // 安全检查：防止同一订单多次回滚库存（通过分布式锁+计数双重保护）
                if (returnRequestMapper.countCompletedByOrderId(record.getOrderId()) > 1) {
                    log.warn("订单已存在已完成的退货记录，跳过库存回滚, orderId={}", record.getOrderId());
                } else {
                    if (order.getCanSupply() != null && order.getCanSupply() == 1) {
                        List<OrderItem> items = orderItemMapper.selectByOrderId(record.getOrderId());
                        for (OrderItem item : items) {
                            productMapper.updateStock(item.getProductCode(), item.getQuantity());
                            evictProductCache(item.getProductCode());
                        }
                        evictLowStockCache();
                    }
                }
                // 更新订单退款状态：已退款订单状态设为已取消(3)
                if (order.getPaymentStatus() != null && order.getPaymentStatus() == 1) {
                    order.setPaymentStatus(2); // 已退款
                    order.setOrderStatus(3);   // 已取消（已退款）
                    orderMapper.updateById(order);
                } else {
                    // 未付款的订单退货，也标记为已取消
                    order.setOrderStatus(3);
                    orderMapper.updateById(order);
                }
                log.info("退货退款完成, id={}, orderId={}, refundAmount={}", id, record.getOrderId(), record.getRefundAmount());
            }
            returnRequestMapper.complete(id);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("退换货完成操作被中断", e);
        } finally {
            if (!syncRegistered && lock.isHeldByCurrentThread()) {
                try { lock.unlock(); } catch (Exception ignored) {}
            }
        }
    }

    private void evictProductCache(String productCode) {
        Cache cache = cacheManager.getCache("product");
        if (cache != null) cache.evict(productCode);
    }

    private void evictLowStockCache() {
        Cache cache = cacheManager.getCache("lowStockProducts");
        if (cache != null) cache.clear();
    }
}
