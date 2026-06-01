package com.sports.sales.service;

import com.sports.sales.entity.PurchaseRecord;
import java.util.List;

public interface PurchaseRecordService {

    List<PurchaseRecord> list();

    PurchaseRecord getById(Long id);

    boolean add(PurchaseRecord record);

    boolean confirmPurchase(Long id);
}
