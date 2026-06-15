package com.sports.sales.service;

import com.sports.sales.common.PageResult;
import com.sports.sales.entity.Invoice;

public interface InvoiceService {

    PageResult<Invoice> list(String customerCode, Integer status, int pageNum, int pageSize);

    Invoice getById(Long id);

    Invoice getByOrderId(Long orderId);

    /** 顾客请求开票 */
    Long request(Invoice record);

    /** 管理员开票（打印发票） */
    boolean issue(Long id, String invoiceNo);

    /** 取消/作废发票（待开票可取消，已开票可作废） */
    boolean cancel(Long id, String customerCode);
}
