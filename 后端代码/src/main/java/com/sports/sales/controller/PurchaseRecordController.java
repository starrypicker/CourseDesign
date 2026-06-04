package com.sports.sales.controller;

import com.sports.sales.common.Result;
import com.sports.sales.entity.PurchaseRecord;
import com.sports.sales.service.PurchaseRecordService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/purchase")
public class PurchaseRecordController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PurchaseRecordController.class);

    private final PurchaseRecordService purchaseRecordService;

    public PurchaseRecordController(PurchaseRecordService purchaseRecordService) {
        this.purchaseRecordService = purchaseRecordService;
    }

    @GetMapping("/list")
    public Result<List<PurchaseRecord>> list() {
        return Result.success(purchaseRecordService.list());
    }

    @GetMapping("/{id}")
    public Result<PurchaseRecord> getById(@PathVariable Long id) {
        return Result.success(purchaseRecordService.getById(id));
    }

    @PostMapping
    public Result<Void> add(@RequestBody PurchaseRecord record) {
        log.info("收到添加进货记录请求, productCode={}", record.getProductCode());
        return purchaseRecordService.add(record) ? Result.success() : Result.error("添加失败");
    }

    @PutMapping("/confirm/{id}")
    public Result<Void> confirmPurchase(@PathVariable Long id) {
        log.info("收到确认进货请求, id={}", id);
        return purchaseRecordService.confirmPurchase(id) ? Result.success() : Result.error("确认失败");
    }
}
