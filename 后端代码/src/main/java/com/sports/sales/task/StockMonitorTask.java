package com.sports.sales.task;

import com.sports.sales.entity.Product;
import com.sports.sales.entity.PurchaseRecord;
import com.sports.sales.mapper.ProductMapper;
import com.sports.sales.mapper.PurchaseRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

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
            try {
                autoReplenish(product);
            } catch (Exception e) {
                log.error("自动补货失败: productCode={}", product.getProductCode(), e);
            }
        }
        log.info("====== 库存预警检查完成, 共{}件商品库存不足 ======", lowStockProducts.size());
    }

    private void autoReplenish(Product product) {
        int minStock = product.getMinStock() != null ? product.getMinStock() : 10;
        int stockQuantity = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
        int replenishQty = minStock * 3 - stockQuantity;
        if (replenishQty <= 0) {
            log.info("商品[{}]库存已充足，无需补货", product.getProductName());
            return;
        }
        PurchaseRecord record = new PurchaseRecord();
        record.setProductCode(product.getProductCode());
        record.setManufacturerCode(product.getManufacturerCode());
        record.setQuantity(replenishQty);
        record.setUnitPrice(product.getUnitPrice());
        record.setTotalAmount(product.getUnitPrice() != null
                ? product.getUnitPrice().multiply(BigDecimal.valueOf(replenishQty))
                : BigDecimal.ZERO);
        record.setStatus(0);
        purchaseRecordMapper.insert(record);
        log.info("自动创建进货记录: productCode={}, quantity={}", product.getProductCode(), replenishQty);
    }
}
