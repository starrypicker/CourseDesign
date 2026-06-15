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
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ReturnRequestServiceImpl implements ReturnRequestService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ReturnRequestServiceImpl.class);

    private final ReturnRequestMapper returnRequestMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;
    private final InvoiceMapper invoiceMapper;
    private final CacheManager cacheManager;

    public ReturnRequestServiceImpl(ReturnRequestMapper returnRequestMapper,
                                     OrderMapper orderMapper,
                                     OrderItemMapper orderItemMapper,
                                     ProductMapper productMapper,
                                     InvoiceMapper invoiceMapper,
                                     CacheManager cacheManager) {
        this.returnRequestMapper = returnRequestMapper;
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.productMapper = productMapper;
        this.invoiceMapper = invoiceMapper;
        this.cacheManager = cacheManager;
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
        // 仅已完成或已签收的订单可申请退换货
        if (order.getOrderStatus() != 2 && (order.getShippingStatus() == null || order.getShippingStatus() != 2)) {
            throw new RuntimeException("该订单当前状态不支持退换货申请");
        }
        // 检查是否已有进行中的申请
        if (returnRequestMapper.countActiveByOrderId(record.getOrderId()) > 0) {
            throw new RuntimeException("该订单已有一个进行中的退换货申请");
        }
        // 检查是否已完成过退换货处理（防止库存重复回滚）
        if (returnRequestMapper.countCompletedByOrderId(record.getOrderId()) > 0) {
            throw new RuntimeException("该订单已完成退换货处理，无法重复申请");
        }
        // 检查是否已申请发票——已开票的订单需先作废发票才能退换货
        Invoice invoice = invoiceMapper.selectByOrderId(record.getOrderId());
        if (invoice != null) {
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

        if (record.getType() != null && record.getType() == 1) {
            // 换货：不涉及退款，仅标记完成
            log.info("换货申请完成, id={}, orderId={}", id, record.getOrderId());
        } else {
            // 退货退款：回滚库存，更新订单支付状态为已退款
            Orders order = orderMapper.selectById(record.getOrderId());
            if (order == null) {
                throw new RuntimeException("关联订单不存在");
            }
            // 安全检查：防止同一订单多次回滚库存
            if (returnRequestMapper.countCompletedByOrderId(record.getOrderId()) > 1) {
                // 当前这一条还未标记为完成，所以 > 1 表示之前已有完成的退货
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
            // 更新订单退款状态和订单状态
            if (order.getPaymentStatus() != null && order.getPaymentStatus() == 1) {
                order.setPaymentStatus(2); // 已退款
                order.setOrderStatus(2);   // 已完成（已退款）
                orderMapper.updateById(order);
            }
            log.info("退货退款完成, id={}, orderId={}, refundAmount={}", id, record.getOrderId(), record.getRefundAmount());
        }
        returnRequestMapper.complete(id);
        return true;
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
