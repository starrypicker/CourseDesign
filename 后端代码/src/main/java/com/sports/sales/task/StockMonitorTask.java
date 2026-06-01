package com.sports.sales.task;

import com.sports.sales.entity.Product;
import com.sports.sales.entity.PurchaseRecord;
import com.sports.sales.mapper.ProductMapper;
import com.sports.sales.mapper.PurchaseRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockMonitorTask {

    private final ProductMapper productMapper;
    private final PurchaseRecordMapper purchaseRecordMapper;

    @Scheduled(cron = "0 0 8 * * ?")
    public void checkLowStock() {
        log.info("====== 开始库存预警检查 ======");
        List<Product> lowStockProducts = productMapper.selectLowStock();
        if (lowStockProducts.isEmpty()) {
            log.info("所有商品库存充足");
            return;
        }
        for (Product product : lowStockProducts) {
            log.warn("库存预警: 商品[{}] 当前库存[{}], 最低库存线[{}], 厂家[{}]",
                    product.getProductName(), product.getStockQuantity(),
                    product.getMinStock(), product.getManufacturerName());
            autoReplenishAsync(product).thenAccept(result -> {
                if (result) {
                    log.info("已自动创建进货单: 商品[{}], 厂家[{}]", product.getProductName(), product.getManufacturerName());
                }
            });
        }
        log.info("====== 库存预警检查完成, 共{}件商品库存不足 ======", lowStockProducts.size());
    }

    @Async("taskExecutor")
    public CompletableFuture<Boolean> autoReplenishAsync(Product product) {
        try {
            int replenishQty = product.getMinStock() * 3 - product.getStockQuantity();
            PurchaseRecord record = new PurchaseRecord();
            record.setProductCode(product.getProductCode());
            record.setManufacturerCode(product.getManufacturerCode());
            record.setQuantity(replenishQty);
            record.setUnitPrice(product.getUnitPrice());
            record.setTotalAmount(product.getUnitPrice().multiply(BigDecimal.valueOf(replenishQty)));
            record.setStatus(0);
            purchaseRecordMapper.insert(record);
            log.info("自动创建进货记录: productCode={}, quantity={}", product.getProductCode(), replenishQty);
            return CompletableFuture.completedFuture(true);
        } catch (Exception e) {
            log.error("自动补货失败: productCode={}", product.getProductCode(), e);
            return CompletableFuture.completedFuture(false);
        }
    }
}
