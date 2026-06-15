package com.sports.sales.service;

import com.sports.sales.common.PageResult;
import com.sports.sales.entity.PurchaseRecord;

public interface PurchaseRecordService {

    PageResult<PurchaseRecord> list(Integer status, int pageNum, int pageSize);

    PurchaseRecord getById(Long id);

    boolean add(PurchaseRecord record);

    boolean confirmPurchase(Long id);
}
