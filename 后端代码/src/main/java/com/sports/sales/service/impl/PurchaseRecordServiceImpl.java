package com.sports.sales.service.impl;

import com.sports.sales.entity.PurchaseRecord;
import com.sports.sales.mapper.PurchaseRecordMapper;
import com.sports.sales.mapper.ProductMapper;
import com.sports.sales.service.PurchaseRecordService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class PurchaseRecordServiceImpl implements PurchaseRecordService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PurchaseRecordServiceImpl.class);

    private final PurchaseRecordMapper purchaseRecordMapper;
    private final ProductMapper productMapper;
    private final CacheManager cacheManager;
    private final RedissonClient redissonClient;

    public PurchaseRecordServiceImpl(PurchaseRecordMapper purchaseRecordMapper, ProductMapper productMapper, CacheManager cacheManager, RedissonClient redissonClient) {
        this.purchaseRecordMapper = purchaseRecordMapper;
        this.productMapper = productMapper;
        this.cacheManager = cacheManager;
        this.redissonClient = redissonClient;
    }

    @Override
    public List<PurchaseRecord> list(Integer status) {
        return purchaseRecordMapper.selectList(status);
    }

    @Override
    public PurchaseRecord getById(Long id) {
        return purchaseRecordMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean add(PurchaseRecord record) {
        log.info("添加进货记录, productCode={}, quantity={}", record.getProductCode(), record.getQuantity());
        record.setStatus(0);
        boolean result = purchaseRecordMapper.insert(record) > 0;
        if (result) {
            log.info("进货记录添加成功, id={}", record.getId());
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean confirmPurchase(Long id) {
        String lockKey = "lock:purchase:confirm:" + id;
        RLock lock = redissonClient.getLock(lockKey);
        boolean syncRegistered = false;
        try {
            boolean locked = lock.tryLock(10, 30, TimeUnit.SECONDS);
            if (!locked) {
                throw new RuntimeException("系统繁忙，请稍后重试");
            }
            // 注册事务同步回调：事务提交后再释放锁，防止锁提前释放导致数据不一致
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (lock.isHeldByCurrentThread()) {
                        try {
                            lock.unlock();
                        } catch (Exception ignored) {
                        }
                    }
                }
            });
            syncRegistered = true;

            PurchaseRecord record = purchaseRecordMapper.selectById(id);
            if (record == null) {
                throw new RuntimeException("进货记录不存在");
            }
            if (record.getStatus() != 0) {
                throw new RuntimeException("进货记录状态不正确");
            }
            purchaseRecordMapper.updateStatus(id, 1);
            productMapper.updateStock(record.getProductCode(), record.getQuantity());
            evictProductCache(record.getProductCode());
            log.info("进货确认完成, id={}, productCode={}, quantity={}", id, record.getProductCode(), record.getQuantity());
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("确认入库操作被中断", e);
        } finally {
            if (!syncRegistered && lock.isHeldByCurrentThread()) {
                try {
                    lock.unlock();
                } catch (Exception ignored) {
                }
            }
        }
    }

    /** 精确清除受影响的商品缓存和低库存列表缓存 */
    private void evictProductCache(String productCode) {
        Cache productCache = cacheManager.getCache("product");
        if (productCache != null) {
            productCache.evict(productCode);
        }
        Cache lowStockCache = cacheManager.getCache("lowStockProducts");
        if (lowStockCache != null) {
            lowStockCache.clear();
        }
    }
}
