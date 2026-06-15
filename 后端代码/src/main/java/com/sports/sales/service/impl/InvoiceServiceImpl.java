package com.sports.sales.service.impl;

import com.sports.sales.common.PageResult;
import com.sports.sales.entity.Invoice;
import com.sports.sales.entity.Orders;
import com.sports.sales.mapper.InvoiceMapper;
import com.sports.sales.mapper.OrderMapper;
import com.sports.sales.service.InvoiceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InvoiceServiceImpl implements InvoiceService {

    private static final Logger log = LoggerFactory.getLogger(InvoiceServiceImpl.class);

    private final InvoiceMapper invoiceMapper;
    private final OrderMapper orderMapper;

    public InvoiceServiceImpl(InvoiceMapper invoiceMapper, OrderMapper orderMapper) {
        this.invoiceMapper = invoiceMapper;
        this.orderMapper = orderMapper;
    }

    @Override
    public PageResult<Invoice> list(String customerCode, Integer status, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        Long total = invoiceMapper.selectCount(customerCode, status);
        List<Invoice> rows = invoiceMapper.selectList(customerCode, status, offset, pageSize);
        rows.forEach(r -> {
            r.setStatusName(statusName(r.getStatus()));
        });
        return new PageResult<>(total, rows, pageNum, pageSize);
    }

    @Override
    public Invoice getById(Long id) {
        Invoice inv = invoiceMapper.selectById(id);
        if (inv != null) {
            inv.setStatusName(statusName(inv.getStatus()));
        }
        return inv;
    }

    @Override
    public Invoice getByOrderId(Long orderId) {
        Invoice inv = invoiceMapper.selectByOrderId(orderId);
        if (inv != null) {
            inv.setStatusName(statusName(inv.getStatus()));
        }
        return inv;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long request(Invoice record) {
        // 校验订单存在且属于当前用户
        Orders order = orderMapper.selectById(record.getOrderId());
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!record.getCustomerCode().equals(order.getCustomerCode())) {
            throw new RuntimeException("无权操作该订单");
        }
        // 仅已支付或已完成的订单可申请发票
        if (order.getPaymentStatus() == null || order.getPaymentStatus() == 0) {
            throw new RuntimeException("订单未付款，无法申请发票");
        }
        // 检查是否已申请过（含已作废，作废后不可重开）
        Invoice existing = invoiceMapper.selectByOrderId(record.getOrderId());
        if (existing != null) {
            if (existing.getStatus() != null && existing.getStatus() == 2) {
                throw new RuntimeException("该订单的发票已作废，不可重新申请");
            }
            throw new RuntimeException("该订单已申请过发票，发票号：" + (existing.getInvoiceNo() != null ? existing.getInvoiceNo() : "待生成"));
        }
        record.setAmount(order.getTotalAmount());
        invoiceMapper.insert(record);
        log.info("发票申请已提交, id={}, orderId={}, title={}", record.getId(), record.getOrderId(), record.getInvoiceTitle());
        return record.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean issue(Long id, String invoiceNo) {
        Invoice record = invoiceMapper.selectById(id);
        if (record == null) {
            throw new RuntimeException("发票申请不存在");
        }
        if (record.getStatus() != null && record.getStatus() == 1) {
            throw new RuntimeException("该发票已开具，无需重复操作");
        }
        if (record.getStatus() != null && record.getStatus() == 2) {
            throw new RuntimeException("该发票已作废，无法开具");
        }
        String finalNo = invoiceNo != null && !invoiceNo.isEmpty()
                ? invoiceNo
                : "INV-" + System.currentTimeMillis();
        invoiceMapper.updateStatus(id, 1, finalNo);
        log.info("发票已开具, id={}, invoiceNo={}", id, finalNo);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancel(Long id, String customerCode) {
        Invoice record = invoiceMapper.selectById(id);
        if (record == null) {
            throw new RuntimeException("发票申请不存在");
        }
        if (record.getStatus() != null && record.getStatus() == 2) {
            throw new RuntimeException("该发票已作废，无需重复操作");
        }
        // 校验权限：只能取消自己的发票
        if (customerCode != null && !customerCode.equals(record.getCustomerCode())) {
            throw new RuntimeException("无权操作该发票");
        }
        invoiceMapper.cancelInvoice(id);
        String action = record.getStatus() != null && record.getStatus() == 1 ? "作废" : "取消";
        log.info("发票已{}, id={}, orderId={}", action, id, record.getOrderId());
        return true;
    }

    private String statusName(Integer status) {
        if (status == null) return "待开票";
        return switch (status) {
            case 1 -> "已开票";
            case 2 -> "已作废";
            default -> "待开票";
        };
    }
}
